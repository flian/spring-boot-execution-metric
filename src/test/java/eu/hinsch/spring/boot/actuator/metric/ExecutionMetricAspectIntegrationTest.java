package eu.hinsch.spring.boot.actuator.metric;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.OutputCapture;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Created by lh on 29/04/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ExecutionMetricAspectIntegrationTest.TestConfig.class)
@DirtiesContext
public class ExecutionMetricAspectIntegrationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
        @Bean
        public TestBean testBean() {
            return new TestBean();
        }

        @Bean
        public TestInterface testBeanWithInterface() {
            return new TestBeanWithInterface();
        }

        @Bean
        @Primary
        public CounterService counterService() {
            return Mockito.mock(CounterService.class);
        }

    }

    public interface TestInterface {
        void interfaceMethod();
    }

    public static class TestBean { // implements TestInterface { // TODO why is this not working???

        @ExecutionMetric("class-method")
        public void classMethod() {}

        @ExecutionMetric(value = "logged-method", loglevel = LogLevel.INFO)
        public void loggerMethod() {}

        @ExecutionMetric("errorMethod")
        public void errorMethod() {
            throw new RuntimeException("test exception");
        }

    }

    public static class TestBeanWithInterface implements TestInterface {

        @Override
        @ExecutionMetric("interface-method")
        public void interfaceMethod() {}

    }

    @Autowired
    private TestBean testBean;

    @Autowired
    private TestInterface testBeanWithInterface;

    @Autowired
    private CounterService counterService;

    @Rule
    public OutputCapture output = new OutputCapture();

    @Test
    public void shouldMeasureClassMethod() {
        // when
        testBean.classMethod();

        // then
        verify(counterService).increment("class-method");
    }

    @Test
    public void shouldLogMessage() {
        // when
        testBean.loggerMethod();

        // then
        assertThat(output.toString(), containsString("IntegrationTest$TestBean : Executing logged-method took"));
    }

    @Test
    public void shouldMeasureInterfaceMethod() {
        // when
        testBeanWithInterface.interfaceMethod();

        // then
        verify(counterService).increment("interface-method");
    }

    @Test
    public void shouldPassThroughException() {
        // given
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("test exception");

        // when
        testBean.errorMethod();

        // then -> exception
    }
}
