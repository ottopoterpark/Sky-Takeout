package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;

public interface DishService extends IService<Dish> {
    void saveDish(Dish dish,DishDTO dishDTO);
}
