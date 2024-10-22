package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    void save(Setmeal setmeal, List<SetmealDish> setmealDishes);
}
