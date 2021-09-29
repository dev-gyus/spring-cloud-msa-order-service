package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.vo.RequestOrder;
import com.example.orderservice.vo.ResponseOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/order-service")
public class OrderController {
    private final Environment env;
    private final OrderService orderService;
    private final KafkaProducer kafkaProducer;

    @GetMapping("/health_check")
    public String status(){
        return String.format("it's working in user service on Port %s", env.getProperty("local.server.port"));
    }

    @PostMapping("/{userId}/orders")
    public ResponseEntity<ResponseOrder> createUser(@PathVariable String userId, @Valid @RequestBody RequestOrder requestOrder){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderDto reqDto = modelMapper.map(requestOrder, OrderDto.class);
        reqDto.setUserId(userId);
        OrderDto dto = orderService.createOrder(reqDto);

        ResponseOrder responseOrder = modelMapper.map(dto, ResponseOrder.class);

        /* send this order to the kafka */
        kafkaProducer.send("example-catalog-topic", dto);

        return new ResponseEntity<>(responseOrder, HttpStatus.CREATED);
    }
//
    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<ResponseOrder>> createUser(@PathVariable String userId){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        Iterable<OrderEntity> orderEntities = orderService.getOrdersByUserId(userId);
        List<ResponseOrder> responseOrders = new ArrayList<>();
        orderEntities.forEach(entity -> responseOrders.add(modelMapper.map(entity, ResponseOrder.class)));

        return new ResponseEntity<>(responseOrders, HttpStatus.CREATED);
    }
}
