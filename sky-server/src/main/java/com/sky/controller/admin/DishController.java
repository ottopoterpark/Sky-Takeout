package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminDishController")
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
    public Result save(@RequestBody DishDTO dishDTO)
    {
        log.info("新增菜品:{}",dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishService.saveDish(dish,dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param query
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO query)
    {
        log.info("菜品分页查询:{}",query);
        PageResult pageResult = dishService.pageQuery(query);
        return Result.success(pageResult);
    }

    /**
     * 菜品起售、停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result changeStatus(@PathVariable Integer status,Long id)
    {
        log.info("菜品起售、停售:{} {}",status,id);
        dishService.lambdaUpdate()
                .eq(Dish::getId,id)
                .set(Dish::getStatus,status)
                .update();
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id)
    {
        log.info("根据id查询菜品:{}",id);
        DishVO data=dishService.getDishById(id);
        return Result.success(data);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Dish>> listById(Long categoryId)
    {
        log.info("根据分类id查询菜品:{}",categoryId);
        List<Dish> dishes = dishService.lambdaQuery().eq(Dish::getCategoryId, categoryId).list();
        return Result.success(dishes);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO)
    {
        log.info("修改菜品:{}",dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        dishService.updateWithFlavor(dish,flavors);
        return Result.success();
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids)
    {
        log.info("批量删除菜品:{}",ids);
        dishService.removeBatchWithFlavorsByIds(ids);
        return Result.success();
    }
}
