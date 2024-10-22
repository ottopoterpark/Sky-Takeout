package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    void save(Setmeal setmeal, List<SetmealDish> setmealDishes);

    PageResult pageWithCategoryName(SetmealPageQueryDTO setmealPageQueryDTO);

    SetmealVO get(Long id);

    void update(Setmeal setmeal, List<SetmealDish> setmealDishes);

    void delete(List<Long> ids);
}
