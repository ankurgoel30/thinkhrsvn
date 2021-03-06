package com.thinkhr.external.api.controllers;

import static com.thinkhr.external.api.utils.ApiTestDataUtil.API_BASE_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.thinkhr.external.api.ApiApplication;
import com.thinkhr.external.api.services.HealthCheckService;
import com.thinkhr.external.api.standard.controllers.HealthCheckController;

/**
 * Junit class to test all the methods\APIs written for HealthCheckController
 * 
 * @author Sudhakar Kaki
 * @since 2017-11-18
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ApiApplication.class)
@SpringBootTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class HealthCheckControllerTest {
    private MockMvc mockMvc;

    @MockBean
    private HealthCheckService healthCheckService;

    @InjectMocks
    private HealthCheckController healthCheckController;

    @Autowired
    private WebApplicationContext wac;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }


    /**
     * Test to verify Get HealthCheck API (/v1/healthcheck/{companyId}).
     * 
     * @throws Exception
     */
    @Test
    public void testHealthCheckAPI() throws Exception {
        mockMvc.perform(get(API_BASE_PATH + "healthcheck/1")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    }
}
