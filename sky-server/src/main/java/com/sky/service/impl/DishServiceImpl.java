package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.exception.BaseException;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

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

        if(dishes==null||dishes.size()==0)
            return PageResult.builder()
                    .total(page.getTotal())
                    .records(null)
                    .build();

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
}
