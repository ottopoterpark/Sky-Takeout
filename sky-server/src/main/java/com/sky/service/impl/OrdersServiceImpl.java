package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.OrdersMapper;
import com.sky.service.OrdersService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO)
    {
        // 查询当前用户
        Long userId = BaseContext.getCurrentId();

        // 查询地址信息
        AddressBook addressBook = Db.getById(ordersSubmitDTO.getAddressBookId(), AddressBook.class);

        // 查询购物车数据
        List<ShoppingCart> shoppingCarts = Db.lambdaQuery(ShoppingCart.class).eq(ShoppingCart::getUserId, userId).list();

        // 向订单表中插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setAddress(addressBook.getDetail());
        orders.setOrderTime(LocalDateTime.now());
        orders.setConsignee(addressBook.getConsignee());
        orders.setDeliveryStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(userId);
        save(orders);

        // 订单明细对象
        List<OrderDetail> orderDetails = new ArrayList<>();

        // 向订单明细表中插入若干条数据
        shoppingCarts.stream().forEach(s ->
        {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(s, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        });
        Db.saveBatch(orderDetails);

        // 清空当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId);
        shoppingCartService.remove(wrapper);

        // 封装返回结果
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }
}
