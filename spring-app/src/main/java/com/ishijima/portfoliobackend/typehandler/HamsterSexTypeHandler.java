package com.ishijima.portfoliobackend.typehandler;

import com.ishijima.portfoliobackend.entity.HamsterSex;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(HamsterSex.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class HamsterSexTypeHandler extends BaseTypeHandler<HamsterSex> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, HamsterSex parameter, JdbcType jdbcType) throws SQLException {
		ps.setString(i, parameter.name());
	}

	@Override
	public HamsterSex getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return HamsterSex.fromDatabaseValue(rs.getString(columnName));
	}

	@Override
	public HamsterSex getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return HamsterSex.fromDatabaseValue(rs.getString(columnIndex));
	}

	@Override
	public HamsterSex getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return HamsterSex.fromDatabaseValue(cs.getString(columnIndex));
	}
}
