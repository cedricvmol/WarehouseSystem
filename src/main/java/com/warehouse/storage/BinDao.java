package com.warehouse.storage;

import com.warehouse.domain.Bin;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BinDao {

    private Connection connection;

    public BinDao(Connection connection){
        this.connection = connection;
    }

    public void insert(Bin bin) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("INSERT INTO bins (binId,locationCode) VALUES (?,?)"))
        {
            ps.setInt(1,bin.getBinId());
            ps.setString(2,bin.getLocationCode());

            ps.executeUpdate();
        }
    }

    public Bin findById(int binId) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM bins WHERE binId = ?"))
        {
            ps.setInt(1,binId);
            try(ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    String locationCode = rs.getString("locationCode");

                    Bin bin = new Bin(binId,locationCode);
                    return  bin;
                }
            }
        }
        throw new IllegalArgumentException("No bin found with id: " + binId + ".");
    }

    public List<Bin> findAll() throws SQLException{
        List<Bin> bins = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM bins");
            ResultSet rs = ps.executeQuery())
        {
            while (rs.next()){
                int binId = rs.getInt("binId");
                String locationCode = rs.getString("locationCode");

                Bin bin = new Bin(binId,locationCode);
                bins.add(bin);
            }
        }
        return bins;
    }

    public void update(Bin bin) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("UPDATE bins SET locationCode = ? WHERE binId = ?"))
        {
            ps.setString(1,bin.getLocationCode());
            ps.setInt(2,bin.getBinId());

            ps.executeUpdate();
        }
    }

    public void delete(int binId) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM bins WHERE binId = ?"))
        {
            ps.setInt(1,binId);
            ps.executeUpdate();
        }
    }
}
