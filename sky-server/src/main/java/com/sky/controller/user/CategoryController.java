package com.sky.controller.user;

import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端分类接口
 */
@RestController("userCategoryController")
@RequestMapping("/user/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 分类条件查询
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type)
    {
        List<Category> categories = categoryService.lambdaQuery().eq(type != null, Category::getType, type).list();
        return Result.success(categories);
    }
}
