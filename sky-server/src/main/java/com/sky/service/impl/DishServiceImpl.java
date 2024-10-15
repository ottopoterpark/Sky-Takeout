package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.exception.BaseException;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
}
