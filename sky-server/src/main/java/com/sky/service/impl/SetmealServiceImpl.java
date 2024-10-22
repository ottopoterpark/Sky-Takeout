package com.sky.service.impl;

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
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    /**
     * 新增套餐
     *
     * @param setmeal
     * @param setmealDishes
     */
    @Override
    @Transactional
    @AutoFill(OperationType.INSERT)
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
        if(setmeals==null||setmeals.size()==0)
            return PageResult.builder().total(0).records(null).build();

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
}
