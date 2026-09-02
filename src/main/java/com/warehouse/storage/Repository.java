package com.warehouse.storage;

import java.sql.SQLException;
import java.util.List;



public interface Repository<T,ID>{

    void insert(T item) throws SQLException;
    T findById(ID id) throws SQLException;
    List<T> findAll() throws SQLException;
    void update(T item) throws SQLException;
    void delete(ID id) throws SQLException;

}
