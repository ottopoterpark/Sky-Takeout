package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.awt.desktop.QuitEvent;

@RestController
@RequestMapping("/admin/category")
@Slf4j
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分类分页查询
     * @param query
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(CategoryPageQueryDTO query)
    {
        log.info("分类分页查询:{}",query);
        Page<Category> p = Page.of(query.getPage(), query.getPageSize());
        p.addOrder(OrderItem.asc("sort"));
        p=categoryService.lambdaQuery()
                .like(query.getName()!=null, Category::getName,query.getName())
                .eq(query.getType()!=null, Category::getType,query.getType())
                .page(p);
        PageResult data = PageResult.builder()
                .total(p.getTotal())
                .records(p.getRecords())
                .build();
        return Result.success(data);
    }

    /**
     * 修改分类
     * @param query
     * @return
     */
    @PutMapping
    @Transactional
    public Result update(@RequestBody CategoryDTO query)
    {
        log.info("修改分类:{}",query);
        categoryService.lambdaUpdate()
                .eq(Category::getId,query.getId())
                .set(Category::getName,query.getName())
                .set(Category::getSort,query.getSort())
                .set(Category::getType,query.getType())
                .update();
        return Result.success();
    }

    /**
     * 启用，禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result changeStatus(@PathVariable Integer status,Long id)
    {
        log.info("启用，禁用分类:{} {}",status,id);
        categoryService.lambdaUpdate()
                .eq(Category::getId,id)
                .set(Category::getStatus,status)
                .update();
        return Result.success();
    }
}
