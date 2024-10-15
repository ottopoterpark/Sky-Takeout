package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.BaseException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.awt.desktop.QuitEvent;
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
        log.info("菜品分页查询");
        Page<Dish> page = Page.of(query.getPage(), query.getPageSize());
        dishService.lambdaQuery()
            .like(query.getName() != null, Dish::getName, query.getName())
            .eq(query.getCategoryId() != null, Dish::getCategoryId, query.getCategoryId())
            .eq(query.getStatus() != null, Dish::getStatus, query.getStatus())
            .page(page);
        PageResult data = PageResult.builder()
                .total(page.getTotal())
                .records(page.getRecords())
                .build();
        return Result.success(data);
    }
}
