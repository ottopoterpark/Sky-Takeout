package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        // 查询套餐关联的菜品
        Set<Long> dishIds = Db.lambdaQuery(SetmealDish.class).eq(SetmealDish::getSetmealId, id).list()
                .stream().map(SetmealDish::getDishId).collect(Collectors.toSet());

        // 查询关联菜品的销售状态
        Set<Integer> dishStatus = Db.lambdaQuery(Dish.class).in(Dish::getId, dishIds).list()
                .stream().map(Dish::getStatus).collect(Collectors.toSet());

        // 如果包含未起售菜品，则该套餐无法起售
        if (dishStatus.contains(StatusConstant.DISABLE))
            return Result.error(MessageConstant.SETMEAL_ENABLE_FAILED);

        // 更新套餐状态
        setmealService.lambdaUpdate()
                .eq(Setmeal::getId,id)
                .set(Setmeal::getStatus,status)
                .update();
        return Result.success();
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<SetmealVO> get(@PathVariable Long id)
    {
        log.info("根据id查询套餐:{}",id);
        SetmealVO setmealVOS=setmealService.get(id);
        return Result.success(setmealVOS);
    }

    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @PutMapping
    public Result update(@RequestBody SetmealDTO setmealDTO)
    {
        log.info("修改套餐:{}",setmealDTO);
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealService.update(setmeal,setmealDishes);
        return Result.success();
    }

    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids)
    {
        log.info("批量删除套餐:{}",ids);

        // 查询套餐的销售状态
        Set<Integer> setmealStatus = setmealService.lambdaQuery().in(Setmeal::getId, ids).list()
                .stream().map(Setmeal::getStatus).collect(Collectors.toSet());

        // 如果套餐启售中无法删除
        if(setmealStatus.contains(StatusConstant.ENABLE))
            return Result.error(MessageConstant.SETMEAL_ON_SALE);

        setmealService.delete(ids);
        return Result.success();
    }
}
