package com.ishijima.portfoliobackend.typehandler;

import com.ishijima.portfoliobackend.entity.HamsterStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(HamsterStatus.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class HamsterStatusTypeHandler extends BaseTypeHandler<HamsterStatus> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, HamsterStatus parameter, JdbcType jdbcType) throws SQLException {
		ps.setString(i, parameter.name());
	}

	@Override
	public HamsterStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return HamsterStatus.fromDatabaseValue(rs.getString(columnName));
	}

	@Override
	public HamsterStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return HamsterStatus.fromDatabaseValue(rs.getString(columnIndex));
	}

	@Override
	public HamsterStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return HamsterStatus.fromDatabaseValue(cs.getString(columnIndex));
	}
}
