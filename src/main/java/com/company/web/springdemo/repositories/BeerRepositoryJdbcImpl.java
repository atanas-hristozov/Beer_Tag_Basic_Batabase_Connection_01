package com.company.web.springdemo.repositories;

import com.company.web.springdemo.exceptions.EntityNotFoundException;
import com.company.web.springdemo.models.Beer;
import com.company.web.springdemo.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@PropertySource("classpath:application.properties")
public class BeerRepositoryJdbcImpl implements BeerRepository{
    private final String dbUrl, dbUsername, dbPassword;

    @Autowired
    public BeerRepositoryJdbcImpl(Environment env){
        dbUrl = env.getProperty("database.url");
        dbUsername = env.getProperty("database.username");
        dbPassword = env.getProperty("database.password");
    }


    @Override
    public List<Beer> get(String name, Double minAbv, Double maxAbv, Integer styleId, String sortBy, String sortOrder) {
        String query= "SELECT * FROM beers.beers";
        try (Connection connection = DriverManager.getConnection(dbUrl,dbUsername,dbPassword);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query);
        ) {
            List<Beer> result = readBeersData(resultSet);
            return filter(result, name, minAbv, maxAbv, styleId, sortBy, sortOrder);
            //return result;
        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public Beer get(int id) {
        String query= "SELECT beer_id, name, abv FROM beers.beers where beer_id = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl,dbUsername,dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ) {
                preparedStatement.setInt(1, id);
                try(ResultSet resultSet = preparedStatement.executeQuery()){
                    List<Beer> result = readBeersData(resultSet);
                    if (result.size()==0){
                        throw new EntityNotFoundException("Beer", id);
                    }
                    return result.get(0);
            }
        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }



    @Override
    public Beer get(String name) {
        String query= "SELECT beer_id, name, abv FROM beers.beers where name = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl,dbUsername,dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
        ) {
            preparedStatement.setString(1, name);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                List<Beer> result = readBeersData(resultSet);
                if (result.size()==0){
                    throw new EntityNotFoundException("Beer", "name", name);
                }
                return result.get(0);
            }
        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(Beer beer, User user) {
        String query = "INSERT INTO beers.beers (name, abv) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(dbUrl,dbUsername,dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
        ) {
            preparedStatement.setString(1, beer.getName());
            preparedStatement.setDouble(2, beer.getAbv());
            preparedStatement.executeUpdate();

        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Beer beer) {
        String query = "UPDATE beers.beers SET name = ?, abv = ? WHERE beer_id = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl,dbUsername,dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
        ) {
            preparedStatement.setString(1, beer.getName());
            preparedStatement.setDouble(2, beer.getAbv());
            preparedStatement.setInt(3, beer.getId());
            preparedStatement.executeUpdate();

        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM beers.beers WHERE beer_id = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl,dbUsername,dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
        ) {
            preparedStatement.setInt(3, id);
            preparedStatement.executeUpdate();

        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private List<Beer> readBeersData(ResultSet resultSet) throws SQLException{
        List<Beer> beers = new ArrayList<>();
        while (resultSet.next()){
            Beer beer = new Beer();
            beer.setId(resultSet.getInt("beer_id"));
            beer.setName(resultSet.getString("name"));
            beer.setAbv(resultSet.getDouble("abv"));

            beers.add(beer);
        }
        return beers;
    }

    private static List<Beer> filterByName(List<Beer> beers, String name) {
        if (name != null && !name.isEmpty()) {
            beers = beers.stream()
                    .filter(beer -> containsIgnoreCase(beer.getName(), name))
                    .collect(Collectors.toList());
        }
        return beers;
    }

    private static List<Beer> filterByAbv(List<Beer> beers, Double minAbv, Double maxAbv) {
        if (minAbv != null) {
            beers = beers.stream()
                    .filter(beer -> beer.getAbv() >= minAbv)
                    .collect(Collectors.toList());
        }

        if (maxAbv != null) {
            beers = beers.stream()
                    .filter(beer -> beer.getAbv() <= maxAbv)
                    .collect(Collectors.toList());
        }

        return beers;
    }

    private static List<Beer> filterByStyle(List<Beer> beers, Integer styleId) {
        if (styleId != null) {
            beers = beers.stream()
                    .filter(beer -> beer.getStyle().getId() == styleId)
                    .collect(Collectors.toList());
        }
        return beers;
    }

    private static List<Beer> sortBy(List<Beer> beers, String sortBy) {
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "name":
                    beers.sort(Comparator.comparing(Beer::getName));
                    break;
                case "abv":
                    beers.sort(Comparator.comparing(Beer::getAbv));
                case "style":
                    beers.sort(Comparator.comparing(beer -> beer.getStyle().getName()));
                    break;
            }
        }
        return beers;
    }

    private static List<Beer> order(List<Beer> beers, String order) {
        if (order != null && !order.isEmpty()) {
            if (order.equals("desc")) {
                Collections.reverse(beers);
            }
        }
        return beers;
    }

    private static boolean containsIgnoreCase(String value, String sequence) {
        return value.toLowerCase().contains(sequence.toLowerCase());
    }
    private List<Beer> filter(List<Beer> result,String name, Double minAbv,
                              Double maxAbv, Integer styleId, String sortBy, String sortOrder){
        result = filterByName(result, name);
        result = filterByAbv(result, minAbv, maxAbv);
        result = filterByStyle(result, styleId);
        result = sortBy(result, sortBy);
        result = order(result, sortOrder);
        return result;
    }

}
