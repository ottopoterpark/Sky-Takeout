package com.sky.controller.user;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询套餐
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Setmeal>> list(Long categoryId)
    {
        log.info("根据分类id查询套餐:{}", categoryId);

        // 查询redis中是否存在分类的展示数据
        String key = "category_" + categoryId;
        List<Setmeal> setmeals = (List<Setmeal>) redisTemplate.opsForValue().get(key);

        // 存在则直接返回结果
        if (setmeals!=null)
            return Result.success(setmeals);

        setmeals = setmealService.lambdaQuery()
                .eq(Setmeal::getCategoryId, categoryId)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE)
                .list();

        // redis缓存
        redisTemplate.opsForValue().set(key,setmeals);

        return Result.success(setmeals);
    }

    /**
     * 根据套餐id查询菜品
     *
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    public Result<List<DishItemVO>> dish(@PathVariable Long id)
    {
        log.info("根据套餐id查询菜品:{}", id);

        // 查询该套餐关联的各个菜品id和份数
        Map<Long, Integer> setmealDishes = Db.lambdaQuery(SetmealDish.class).eq(SetmealDish::getSetmealId, id).list()
                .stream().collect(Collectors.toMap(SetmealDish::getDishId, SetmealDish::getCopies));

        // 如果查询结果为空
        if (setmealDishes == null || setmealDishes.size() == 0)
            return Result.success(null);

        // 查询出这些id所对应的菜品
        List<Dish> dishes = Db.lambdaQuery(Dish.class).in(Dish::getId, setmealDishes.keySet()).list();

        // 封装结果
        List<DishItemVO> dishItemVOS = new ArrayList<>();
        for (Dish dish : dishes)
        {
            DishItemVO dishItemVO = new DishItemVO();
            BeanUtils.copyProperties(dish, dishItemVO);
            dishItemVO.setCopies(setmealDishes.get(dish.getId()));
            dishItemVOS.add(dishItemVO);
        }

        // 返回结果
        return Result.success(dishItemVOS);
    }
}
