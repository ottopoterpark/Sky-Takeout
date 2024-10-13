package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
