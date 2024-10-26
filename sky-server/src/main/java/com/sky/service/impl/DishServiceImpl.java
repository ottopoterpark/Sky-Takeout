package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.exception.BaseException;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private  DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dish
     * @param dishDTO
     */
    @Override
    @AutoFill(OperationType.INSERT)
    public void saveDish(Dish dish,DishDTO dishDTO)
    {
        try
        {
            // 保存菜品
            save(dish);
        } catch (Exception e)
        {
            throw new BaseException(MessageConstant.DISHNAME_DUPLICATE);
        }

        // 获取菜品口味和id
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long dishId = dish.getId();

        // 填充菜品id字段
        for (DishFlavor flavor : flavors)
            flavor.setDishId(dishId);

        // 保存菜品口味
        Db.saveBatch(flavors);
    }

    /**
     * 菜品分页查询
     * @param query
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO query)
    {
        // 避免分页参数进入条件查询中
        if(query.getName()!=null||query.getCategoryId()!=null||query.getStatus()!=null)
            query.setPage(1);

        Page<Dish> page = Page.of(query.getPage(), query.getPageSize());

        // 查询符合条件(名称，菜品分类，售卖状态)的Dish
        lambdaQuery()
                .like(query.getName() != null, Dish::getName, query.getName())
                .eq(query.getCategoryId() != null, Dish::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, Dish::getStatus, query.getStatus())
                .page(page);
        List<Dish> dishes = page.getRecords();

        // 如果查询结果为空，直接返回
        if(dishes.isEmpty())
            return PageResult.builder().total(page.getTotal()).records(page.getRecords()).build();

        // 获取这些Dish的categoryId的Set
        Set<Long> catogoryIds = dishes.stream().map(Dish::getCategoryId).collect(Collectors.toSet());

        // 一次性查询出这些菜品关联的分类并将分类id和分类name作为键值对生成Map
        Map<Long, String> map = Db.lambdaQuery(Category.class)
                .in(Category::getId, catogoryIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        ArrayList<DishVO> dishVOS = new ArrayList<>();

        // 将Dish与对应的categoryName封装为DishVO
        for (Dish dish : dishes)
        {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish,dishVO);
            dishVO.setCategoryName(map.get(dish.getCategoryId()));
            dishVOS.add(dishVO);
        }

        // 返回结果
        return PageResult.builder()
                .total(page.getTotal())
                .records(dishVOS)
                .build();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public DishVO getDishById(Long id)
    {
        Dish dish = lambdaQuery().eq(Dish::getId, id).one();
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        Category category = Db.lambdaQuery(Category.class).eq(Category::getId, dish.getCategoryId()).one();
        List<DishFlavor> dishFlavors = Db.lambdaQuery(DishFlavor.class).eq(DishFlavor::getDishId, dish.getId()).list();
        dishVO.setCategoryName(category.getName());
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品
     * @param dish
     * @param flavors
     */
    @Override
    @AutoFill(OperationType.UPDATE)
    @Transactional
    public void updateWithFlavor(Dish dish, List<DishFlavor> flavors)
    {
        // 更新菜品信息
        lambdaUpdate()
                .eq(Dish::getId,dish.getId())
                .set(dish.getName()!=null,Dish::getName,dish.getName())
                .set(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId())
                .set(dish.getPrice()!=null,Dish::getPrice,dish.getPrice())
                .set(dish.getImage()!=null,Dish::getImage,dish.getImage())
                .set(dish.getDescription()!=null,Dish::getDescription,dish.getDescription())
                .set(dish.getStatus()!=null,Dish::getStatus,dish.getStatus())
                .set(dish.getUpdateTime()!=null,Dish::getUpdateTime,dish.getUpdateTime())
                .set(dish.getUpdateUser()!=null,Dish::getUpdateUser,dish.getUpdateUser())
                .update();

        // 删除该菜品关联的所有口味
        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dish.getId());
        dishFlavorService.remove(wrapper);

        // 将口味列表重新插入口味表
        for (DishFlavor flavor : flavors)
        {
            flavor.setDishId(dish.getId());
        }
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<DishVO> listWithCategoryNameAndFlavors(Long categoryId)
    {
        // 查询redis中是否存在分类展示数据
        String key="category_"+categoryId;
        List<DishVO> dishVOS=(List<DishVO>) redisTemplate.opsForValue().get(key);

        // 存在则直接返回结果
        if(dishVOS!=null)
            return dishVOS;

        // 根据分类id查询菜品
        List<Dish> dishes = lambdaQuery()
                .eq(Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus,StatusConstant.ENABLE)
                .list();

        // 根据分类id查询分类名称
        Category category = Db.lambdaQuery(Category.class).eq(Category::getId, categoryId).one();
        String categoryName = category.getName();

        // 根据菜品ids查询对应的口味
        Set<Long> dishIds = dishes.stream().map(Dish::getId).collect(Collectors.toSet());
        Map<Long, List<DishFlavor>> dishFlavors = Db.lambdaQuery(DishFlavor.class)
                .in(dishIds!=null&&dishIds.size()>0,DishFlavor::getDishId, dishIds).list()
                .stream().collect(Collectors.groupingBy(DishFlavor::getDishId));

        // 封装结果
        dishVOS = new ArrayList<>();
        for (Dish dish : dishes)
        {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish,dishVO);
            dishVO.setCategoryName(categoryName);
            dishVO.setFlavors(dishFlavors.get(dish.getId()));
            dishVOS.add(dishVO);
        }

        // 存入redis
        redisTemplate.opsForValue().set(key,dishVOS);

        // 返回结果
        return dishVOS;
    }
}
