package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService extends IService<Dish> {
    void saveDish(Dish dish,DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO query);

    DishVO getDishById(Long id);

    void updateWithFlavor(Dish dish, List<DishFlavor> flavors);

    void removeBatchWithFlavorsByIds(List<Long> ids);
}
