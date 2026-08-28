package com.warehouse.storage;



import com.warehouse.domain.Bin;
import com.warehouse.domain.Product;
import com.warehouse.domain.StockItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockItemDao {

    private Connection connection;
    private BinDao binDao;
    private ProductDao productDao;

    public StockItemDao(Connection connection,BinDao binDao, ProductDao productDao){
        this.connection = connection;
        this.binDao = binDao;
        this.productDao = productDao;
    }

    public void insert(StockItem stockItem) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("INSERT INTO stockItems (productId,binId,quantity) VALUES (?,?,?)"))
        {
            ps.setInt(1,stockItem.getProduct().getProductId());
            ps.setInt(2,stockItem.getBin().getBinId());
            ps.setInt(3,stockItem.getQuantity());

            ps.executeUpdate();
        }
    }

    public StockItem findByProductAndBin(int productId,int binId) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM stockItems WHERE productId = ? AND binId = ?"))
        {
            ps.setInt(1,productId);
            ps.setInt(2,binId);
            try(ResultSet rs = ps.executeQuery())
            {
                if(rs.next()){
                    int quantity = rs.getInt("quantity");

                    StockItem stockItem = new StockItem(productDao.findById(productId),binDao.findById(binId),quantity);
                    return stockItem;
                }
            }
        }
        throw new IllegalArgumentException("No stockItem found with productId: " + productId + " and binId: " + binId + ".");
    }

    public List<StockItem> findByProduct(int productId) throws SQLException{
        List<StockItem> stockItems = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM stockItems WHERE productId = ?"))
        {
            ps.setInt(1,productId);
            try(ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    int quantity = rs.getInt("quantity");
                    int binId = rs.getInt("binId");

                    StockItem stockItem = new StockItem(productDao.findById(productId),binDao.findById(binId),quantity);
                    stockItems.add(stockItem);
                }
            }
        }
        return stockItems;
    }

    public List<StockItem> findAll() throws SQLException {
        List<StockItem> stockItems = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM stockItems");
        ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                int productId = rs.getInt("productId");
                int binId = rs.getInt("binId");
                int quantity = rs.getInt("quantity");

                Product product = productDao.findById(productId);
                Bin bin = binDao.findById(binId);

                StockItem stockItem = new StockItem(product,bin,quantity);
                stockItems.add(stockItem);
            }
        }
        return stockItems;
    }

    public void update(StockItem stockItem) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("UPDATE stockItems SET quantity = ? WHERE productId = ? AND binId = ?"))
        {
            ps.setInt(1,stockItem.getQuantity());
            ps.setInt(2,stockItem.getProduct().getProductId());
            ps.setInt(3,stockItem.getBin().getBinId());

            ps.executeUpdate();
        }
    }

    public void delete(int productId,int binId) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("DELETE from stockItems WHERE productId = ? AND binId = ?"))
        {
            ps.setInt(1,productId);
            ps.setInt(2,binId);

            ps.executeUpdate();
        }
    }




}
