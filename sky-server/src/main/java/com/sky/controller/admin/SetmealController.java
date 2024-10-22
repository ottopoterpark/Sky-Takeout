package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminSetmealController")
@RequestMapping("/admin/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    public Result save(@RequestBody SetmealDTO setmealDTO)
    {
        log.info("新增套餐:{}", setmealDTO);
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealService.save(setmeal,setmealDishes);
        return Result.success();
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO)
    {
        log.info("分页查询:{}",setmealPageQueryDTO);
        PageResult data=setmealService.pageWithCategoryName(setmealPageQueryDTO);
        return Result.success(data);
    }

    /**
     * 套餐状态修改
     * @param status
     * @return
     */
    @PostMapping("/status/{status}")
    public Result status(@PathVariable Integer status,Long id)
    {
        log.info("套餐状态修改:{}",status);
        setmealService.lambdaUpdate()
                .eq(Setmeal::getId,id)
                .set(Setmeal::getStatus,status)
                .update();
        return Result.success();
    }
}
