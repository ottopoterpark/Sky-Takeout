package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    /**
     * 新增套餐
     * @param setmeal
     * @param setmealDishes
     */
    @Override
    @Transactional
    @AutoFill(OperationType.INSERT)
    public void save(Setmeal setmeal, List<SetmealDish> setmealDishes)
    {
        save(setmeal);
        setmealDishes.stream().forEach(s->s.setSetmealId(setmeal.getId()));
        Db.saveBatch(setmealDishes);
    }
}
