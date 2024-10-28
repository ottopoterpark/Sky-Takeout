package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {
    void add(ShoppingCartDTO shoppingCartDTO);
}
