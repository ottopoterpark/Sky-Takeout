package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrdersMapper;
import com.sky.result.PageResult;
import com.sky.service.OrdersService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.webSocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

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


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception
    {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = Db.getById(userId, User.class);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID"))
        {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo)
    {

        // 根据订单号查询订单
        Orders ordersDB = Db.lambdaQuery(Orders.class).eq(Orders::getNumber, outTradeNo).one();

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        Db.lambdaUpdate(Orders.class)
                .eq(Orders::getId, orders.getId())
                .set(Orders::getStatus, orders.getStatus())
                .set(Orders::getPayStatus, orders.getPayStatus())
                .set(Orders::getCheckoutTime, orders.getCheckoutTime())
                .update();

        // 通过WebSocket向客户端推送消息
        Map map=new HashMap();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号:"+outTradeNo);

        // 消息推送
        String json = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 用户催单
     * @param id
     */
    @Override
    public void reminder(Long id)
    {
        // 根据订单id查询订单
        Orders order = Db.getById(id, Orders.class);

        // 封装结果参数
        Map map=new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号："+order.getNumber());

        // 通过WebSocket向客户端发送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 查看订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO one(Long id)
    {
        // 查询订单
        Orders order = getById(id);

        // 查询订单细节
        List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class)
                .eq(OrderDetail::getOrderId, id)
                .list();

        // 封装结果
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(orderDetails);

        // 返回结果
        return orderVO;
    }

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult history(OrdersPageQueryDTO ordersPageQueryDTO)
    {
        // 获取当前用户
        Long userId = BaseContext.getCurrentId();

        // 构造分页参数
        Page<Orders> p = Page.of(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 进行分页条件查询
        lambdaQuery()
                .eq(Orders::getUserId,userId)
                .eq(ordersPageQueryDTO.getStatus()!=null,Orders::getStatus,ordersPageQueryDTO.getStatus())
                .orderByDesc(Orders::getCheckoutTime)
                .page(p);

        // 获取分页查询结果
        long total = p.getTotal();
        List<Orders> orders = p.getRecords();

        // 获取订单id
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();

        // 将订单id与关联的订单细则组成Map
        Map<Long, List<OrderDetail>> orderDetails = Db.lambdaQuery(OrderDetail.class)
                .in(!orderIds.isEmpty(), OrderDetail::getOrderId, orderIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        // 封装结果
        List<OrderVO> orderVOS=new ArrayList<>();
        orders.stream().forEach(o->{
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(o,orderVO);
            orderVO.setOrderDetailList(orderDetails.get(o.getId()));
            orderVOS.add(orderVO);
        });

        // 返回结果
        return PageResult.builder()
                .total(total)
                .records(orderVOS)
                .build();
    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    @Transactional
    public void repeat(Long id)
    {
        // 查询订单明细
        List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class)
                .eq(id != null, OrderDetail::getOrderId, id)
                .list();

        // 将订单菜品套餐重新添加回购物车
        orderDetails.stream().forEach(o->{
            Integer number = o.getNumber();
            for (Integer i = 0; i < number; i++)
            {
                ShoppingCartDTO shoppingCartDTO = new ShoppingCartDTO();
                BeanUtils.copyProperties(o,shoppingCartDTO);
                shoppingCartService.add(shoppingCartDTO);
            }
        });
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    @Transactional
    public void cancel(Long id)
    {
        lambdaUpdate()
                .eq(Orders::getId,id)
                .set(Orders::getStatus,Orders.CANCELLED)
                .update();
    }
}
