package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.naming.Name;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController("adminDishController")
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     *
     * @param dishDTO
     * @return
     */
    @PostMapping
    @CacheEvict(cacheNames = "category", key = "#dishDTO.getCategoryId()")
    public Result save(@RequestBody DishDTO dishDTO)
    {
        log.info("新增菜品:{}", dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishService.saveDish(dish, dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     *
     * @param query
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO query)
    {
        log.info("菜品分页查询:{}", query);
        PageResult pageResult = dishService.pageQuery(query);
        return Result.success(pageResult);
    }

    /**
     * 菜品起售、停售
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "category", allEntries = true)
    public Result changeStatus(@PathVariable Integer status, Long id)
    {
        log.info("菜品起售、停售:{} {}", status, id);

        // 查询菜品关联的套餐
        Set<Long> setmealIds = Db.lambdaQuery(SetmealDish.class).eq(SetmealDish::getDishId, id).list()
                .stream().map(SetmealDish::getSetmealId).collect(Collectors.toSet());

        // 如果有关联套餐
        if (!setmealIds.isEmpty())
        {
            // 查询这些套餐的销售状态
            Set<Integer> setmealStatus = Db.lambdaQuery(Setmeal.class).in(Setmeal::getId, setmealIds).list()
                    .stream().map(Setmeal::getStatus).collect(Collectors.toSet());

            // 如果关联套餐已经起售，则无法将菜品状态设为停售
            if (setmealStatus.contains(StatusConstant.ENABLE))
                return Result.error(MessageConstant.DISH_DISABLE_FAILED);
        }

        // 修改菜品状态
        dishService.lambdaUpdate()
                .eq(Dish::getId, id)
                .set(Dish::getStatus, status)
                .update();

        return Result.success();
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id)
    {
        log.info("根据id查询菜品:{}", id);
        DishVO data = dishService.getDishById(id);
        return Result.success(data);
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Dish>> listById(Long categoryId)
    {
        log.info("根据分类id查询菜品:{}", categoryId);
        List<Dish> dishes = dishService.lambdaQuery().eq(Dish::getCategoryId, categoryId).list();
        return Result.success(dishes);
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     * @return
     */
    @PutMapping
    @CacheEvict(value = {"category", "setmeal"}, allEntries = true)
    public Result update(@RequestBody DishDTO dishDTO)
    {
        log.info("修改菜品:{}", dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        dishService.updateWithFlavor(dish, flavors);
        return Result.success();
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(cacheNames = "category", allEntries = true)
    public Result delete(@RequestParam List<Long> ids)
    {
        log.info("批量删除菜品:{}", ids);

        // 查询ids关联的套餐
        List<Setmeal> setmeals = Db.lambdaQuery(Setmeal.class).in(Setmeal::getCategoryId, ids).list();

        // 如果菜品关联了套餐，则无法删除
        if (setmeals.isEmpty())
            return Result.error(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);

        // 查询菜品的销售状态
        Set<Integer> status = dishService.lambdaQuery().in(Dish::getId, ids).list().stream().map(Dish::getStatus).collect(Collectors.toSet());

        // 如果菜品有起售的无法删除
        if (status.contains(StatusConstant.ENABLE))
            return Result.error(MessageConstant.DISH_ON_SALE);

        // 删除菜品和关联口味
        dishService.removeBatchByIds(ids);
        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<DishFlavor>().in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(wrapper);

        return Result.success();
    }

}
