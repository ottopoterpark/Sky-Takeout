package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    /**
     * 新增购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO)
    {
        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();

        // 购物金额
        BigDecimal price = new BigDecimal(0);

        // 购物
        Setmeal setmeal = null;
        Dish dish = null;

        // 如果添加的是套餐
        if (shoppingCartDTO.getSetmealId() != null)
        {
            setmeal = Db.lambdaQuery(Setmeal.class).eq(Setmeal::getId, shoppingCartDTO.getSetmealId()).one();
            price = setmeal.getPrice();
        }

        // 如果添加的是菜品
        if (shoppingCartDTO.getDishId() != null)
        {
            dish = Db.lambdaQuery(Dish.class).eq(Dish::getId, shoppingCartDTO.getDishId()).one();
            price = dish.getPrice();
        }

        // 查询当前购物车中是否有相同购物
        ShoppingCart shoppingCart = lambdaQuery()
                .eq(ShoppingCart::getUserId, userId)
                .eq(shoppingCartDTO.getDishId() != null, ShoppingCart::getDishId, shoppingCartDTO.getDishId())
                .eq(shoppingCartDTO.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId())
                .eq(shoppingCartDTO.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCartDTO.getDishFlavor())
                .one();

        // 如果存在相同购物，则数量＋1，购物金额增加对应的数额即可
        if (shoppingCart != null)
        {
            Integer number = shoppingCart.getNumber() + 1;
            BigDecimal amount = shoppingCart.getAmount().add(price);

            lambdaUpdate()
                    .eq(ShoppingCart::getUserId, userId)
                    .eq(shoppingCartDTO.getDishId() != null, ShoppingCart::getDishId, shoppingCartDTO.getDishId())
                    .eq(shoppingCartDTO.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId())
                    .eq(shoppingCartDTO.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCartDTO.getDishFlavor())
                    .set(ShoppingCart::getNumber, number)
                    .set(ShoppingCart::getAmount, amount)
                    .update();
            return;
        }

        // 如果不存在相同购物，则新添购物
        shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        // 字段补充
        shoppingCart.setUserId(userId);
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCart.setNumber(1);
        shoppingCart.setAmount(price);

        // 如果购物是套餐
        if (setmeal != null)
        {
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            save(shoppingCart);
        }

        // 如果购物是菜品
        if (dish != null)
        {
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            save(shoppingCart);
        }
    }
}
