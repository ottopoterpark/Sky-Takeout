package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealDishService;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐
     *
     * @param setmeal
     * @param setmealDishes
     */
    @Override
    @Transactional
    @AutoFill(OperationType.INSERT)
    @CacheEvict(value = {"setmeal","category"},allEntries = true)
    public void save(Setmeal setmeal, List<SetmealDish> setmealDishes)
    {
        // 默认新增套餐是停售的
        setmeal.setStatus(0);
        save(setmeal);
        setmealDishes.stream().forEach(s -> s.setSetmealId(setmeal.getId()));
        Db.saveBatch(setmealDishes);
    }

    /**
     * 分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageWithCategoryName(SetmealPageQueryDTO setmealPageQueryDTO)
    {
        // 防止分页参数进入条件查询
        if (setmealPageQueryDTO.getCategoryId() != null || setmealPageQueryDTO.getName() != null || setmealPageQueryDTO.getStatus() != null)
            setmealPageQueryDTO.setPage(1);
        Page<Setmeal> page = Page.of(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        // 分页条件查询
        lambdaQuery()
                .eq(setmealPageQueryDTO.getCategoryId() != null, Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId())
                .like(setmealPageQueryDTO.getName() != null, Setmeal::getName, setmealPageQueryDTO.getName())
                .eq(setmealPageQueryDTO.getStatus() != null, Setmeal::getStatus, setmealPageQueryDTO.getStatus())
                .page(page);
        List<Setmeal> setmeals = page.getRecords();

        // 如果结果为空直接返回
        if(setmeals.isEmpty())
            return PageResult.builder().total(page.getTotal()).records(page.getRecords()).build();

        // 获得套餐对应分类的ids
        Set<Long> categoryIds = setmeals.stream().map(Setmeal::getCategoryId).collect(Collectors.toSet());

        // 通过ids查询对应的分类
        Map<Long, String> categories = Db.lambdaQuery(Category.class).in(Category::getId, categoryIds).list()
                .stream().collect(Collectors.toMap(Category::getId, Category::getName));

        // 封装结果
        List<SetmealVO> setmealVOS = new ArrayList<>();
        for (Setmeal setmeal : setmeals)
        {
            SetmealVO setmealVO = new SetmealVO();
            BeanUtils.copyProperties(setmeal, setmealVO);
            setmealVO.setCategoryName(categories.get(setmeal.getCategoryId()));
            setmealVOS.add(setmealVO);
        }

        // 返回结果
        return PageResult.builder()
                .total(page.getTotal())
                .records(setmealVOS)
                .build();
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO get(Long id)
    {
        Setmeal setmeal = lambdaQuery().eq(Setmeal::getId, id).one();
        Category category = Db.lambdaQuery(Category.class).eq(Category::getId, setmeal.getCategoryId()).one();
        List<SetmealDish> setmealDishes = Db.lambdaQuery(SetmealDish.class).eq(SetmealDish::getSetmealId, setmeal.getId()).list();
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setCategoryName(category.getName());
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmeal
     * @param setmealDishes
     */
    @Override
    @Transactional
    @AutoFill(OperationType.UPDATE)
    @CacheEvict(value = {"setmeal","category"},allEntries = true)
    public void update(Setmeal setmeal, List<SetmealDish> setmealDishes)
    {
        // 更新套餐信息
        lambdaUpdate()
                .eq(Setmeal::getId, setmeal.getId())
                .update(setmeal);

        // 删除与该套餐所有相关的菜品
        LambdaQueryWrapper<SetmealDish> wrapper = new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmeal.getId());
        setmealDishService.remove(wrapper);

        // 为前端传入的所有菜品添加setmealId
        setmealDishes.stream().forEach(s->s.setSetmealId(setmeal.getId()));

        // 重新将前端传入的菜品与该套餐关联
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void delete(List<Long> ids)
    {
        // 删除当前套餐
        removeBatchByIds(ids);

        // 删除套餐菜品关联表中该套餐关联的菜品
        LambdaQueryWrapper<SetmealDish> wrapper = new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(wrapper);
    }
}
