package com.ishijima.portfoliobackend;

import com.ishijima.portfoliobackend.mapper.InquiryMapper;
import com.ishijima.portfoliobackend.mapper.MemberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class PortfolioBackendApplicationTests {

	@MockBean
	private InquiryMapper inquiryMapper;

	@MockBean
	private MemberMapper memberMapper;

	@Test
	void contextLoads() {
	}
}
