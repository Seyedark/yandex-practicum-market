package ru.yandex.practicum.market.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.market.dao.entity.ItemEntity;
import ru.yandex.practicum.market.dao.entity.OrderEntity;
import ru.yandex.practicum.market.service.CoordinatorService;
import ru.yandex.practicum.market.service.ItemService;
import ru.yandex.practicum.market.service.OrderService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ItemController.class)
@ActiveProfiles("test")
@DisplayName("Класс для проверки взаимодействия с контроллером товаров")
public class ItemControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CoordinatorService coordinatorService;

    @Test
    @DisplayName("Проверка метода получения представления главной страницы")
    void getAllItemsByConditionsTest() throws Exception {

        OrderEntity orderEntity = new OrderEntity();
        ItemEntity itemEntity = new ItemEntity();

        itemEntity.setImage("".getBytes(StandardCharsets.UTF_8));

        int page = 1;
        int pageSize = 10;
        String search = "Кот";
        String sort = "NO";

        List<ItemEntity> itemEntityList = List.of(itemEntity);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<ItemEntity> itemEntityPage = new PageImpl<>(itemEntityList, pageable, itemEntityList.size());

        when(orderService.findCartOrder()).thenReturn(orderEntity);
        when(itemService.getAllItemsByConditions(orderEntity, search, sort, page, pageSize)).thenReturn(itemEntityPage);

        mockMvc.perform(get("/")
                        .param("search", search)
                        .param("sortBy", sort)
                        .param("page", Integer.toString(page))
                        .param("pageSize", Integer.toString(pageSize)))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("currentSize"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("sort"))
                .andExpect(model().attributeExists("items"));

        verify(orderService, times(1)).findCartOrder();
        verify(itemService, times(1)).getAllItemsByConditions(orderEntity, search, sort, page, pageSize);
    }

    @Test
    @DisplayName("Проверка метода получения представления страницы товара")
    void getItemByIdTest() throws Exception {
        Long id = 1L;
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setImage("".getBytes(StandardCharsets.UTF_8));

        when(coordinatorService.getItemById(id)).thenReturn(itemEntity);

        mockMvc.perform(get("/item")
                        .param("id", id.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"));

        verify(coordinatorService, times(1)).getItemById(id);
    }
}