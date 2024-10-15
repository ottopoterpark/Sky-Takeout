package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import com.sky.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @Transactional
    public Result save(@RequestBody DishDTO dishDTO)
    {
        log.info("新增菜品:{}",dishDTO);

        // 属性拷贝
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        // 公共字段填充
        dish.setCreateTime(LocalDateTime.now());
        dish.setUpdateTime(LocalDateTime.now());
        dish.setCreateUser(BaseContext.getCurrentId());
        dish.setUpdateUser(BaseContext.getCurrentId());

        try
        {
            // 保存菜品
            dishService.save(dish);
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

        return Result.success();
    }
}
