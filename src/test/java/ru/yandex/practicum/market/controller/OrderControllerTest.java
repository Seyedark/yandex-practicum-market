package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.enums.ActionEnum;
import ru.yandex.practicum.market.enums.OrderStatusEnum;
import ru.yandex.practicum.market.service.CoordinatorService;
import ru.yandex.practicum.market.service.OrderService;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
@DisplayName("Класс для проверки взаимодействия с контроллером заказов")
public class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CoordinatorService coordinatorService;

    @Test
    @DisplayName("Проверка метода получения представления корзины")
    void getCartTest() throws Exception {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setTotalAmount(BigDecimal.ONE);

        when(orderService.findCartOrder()).thenReturn(orderEntity);

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attributeExists("empty"));

        verify(orderService, times(1)).findCartOrder();
    }

    @Test
    @DisplayName("Проверка метода изменения кол-ва товара в заказе и редиректа на главную страницу")
    void changeItemsInOrderTest() throws Exception {
        Long id = 1L;
        String action = ActionEnum.PLUS.name();
        String form = "main";
        mockMvc.perform(post("/cart/items/" + id)
                        .param("action", action)
                        .param("form", form))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(coordinatorService, times(1)).changeItemsInOrder(id, action);
    }

    @Test
    @DisplayName("Проверка метода получения представления завершенного заказа")
    void buyTest() throws Exception {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setTotalAmount(BigDecimal.ONE);

        when(orderService.closeOrder()).thenReturn(orderEntity);

        mockMvc.perform(post("/buy"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("items"));

        verify(orderService, times(1)).closeOrder();
    }

    @Test
    @DisplayName("Проверка метода получения представления всех завершенных заказов")
    void getAllOrdersTest() throws Exception {

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));

        verify(orderService, times(1)).findOrderByStatus(OrderStatusEnum.ORDER.name());
    }

    @Test
    @DisplayName("Проверка метода получения конкретного завершенного заказа")
    void getClosedOrderTest() throws Exception {
        Long id = 1L;

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setTotalAmount(BigDecimal.ONE);

        when(orderService.findById(id)).thenReturn(orderEntity);

        mockMvc.perform(get("/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("order"));

        verify(orderService, times(1)).findById(id);
    }
}