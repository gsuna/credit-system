package com.loan.mappers;

import java.util.List;

public interface BaseMapper<E, D> {

	E toEntity(D dto);
	List<E> toEntityList(List<D> dtoList);
	D toDto(E entity);
	List<D> toDtoList(List<E> entityList);

}
