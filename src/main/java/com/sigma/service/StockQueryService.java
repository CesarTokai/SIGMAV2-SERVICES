package com.sigma.service;

import com.sigma.dto.ProductStockView;
import com.sigma.dto.StockMatrixRow;
import com.sigma.dto.StockRow;
import com.sigma.repository.StockRepository;
import com.sigma.repository.StockView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {
    private final WarehouseAccessService warehouseAccessService;
    private final StockRepository stockRepository;

    public List<StockRow> getCurrentStock() {
        List<Long> accessibleWarehouseIds = warehouseAccessService.getAccessibleWarehouses()
            .stream()
            .map(Warehouse::getId)
            .collect(Collectors.toList());

        List<StockView> stockViews = stockRepository.findCurrentStockByWarehouses(accessibleWarehouseIds);
        return stockViews.stream()
            .map(this::toStockRow)
            .collect(Collectors.toList());
    }

    private StockRow toStockRow(StockView view) {
        StockRow row = new StockRow();
        row.setProductId(view.getProductId());
        row.setProductName(view.getProductName());
        row.setWarehouseId(view.getWarehouseId());
        row.setWarehouseName(view.getWarehouseName());
        row.setQuantity(view.getQuantity());
        row.setUnit(view.getUnit());
        return row;
    }

    public List<StockMatrixRow> getStockMatrix() {
        List<Long> accessibleWarehouseIds = warehouseAccessService.getAccessibleWarehouses()
            .stream()
            .map(Warehouse::getId)
            .collect(Collectors.toList());

        List<Object[]> products = stockRepository.findDistinctProductsByWarehouses(accessibleWarehouseIds);
        List<StockView> allStock = stockRepository.findCurrentStockByWarehouses(accessibleWarehouseIds);

        // Agrupar el stock por producto
        Map<Long, List<StockView>> stockByProduct = allStock.stream()
            .collect(Collectors.groupingBy(StockView::getProductId));

        return products.stream()
            .map(product -> createMatrixRow(product, stockByProduct))
            .collect(Collectors.toList());
    }

    private StockMatrixRow createMatrixRow(Object[] product, Map<Long, List<StockView>> stockByProduct) {
        Long productId = (Long) product[0];
        String productName = (String) product[1];

        StockMatrixRow row = new StockMatrixRow();
        row.setProductId(productId);
        row.setProductName(productName);

        Map<Long, Integer> warehouseQuantities = stockByProduct.getOrDefault(productId, new ArrayList<>())
            .stream()
            .collect(Collectors.toMap(
                StockView::getWarehouseId,
                StockView::getQuantity
            ));

        row.setWarehouseQuantities(warehouseQuantities);

        List<StockView> productStock = stockByProduct.get(productId);
        if (productStock != null && !productStock.isEmpty()) {
            row.setUnit(productStock.get(0).getUnit());
        }

        return row;
    }

    public List<ProductStockView> getAllStock() {
        List<Long> accessibleWarehouseIds = warehouseAccessService.getAccessibleWarehouses()
            .stream()
            .map(Warehouse::getId)
            .collect(Collectors.toList());

        List<StockView> stockViews = stockRepository.findCurrentStockByWarehouses(accessibleWarehouseIds);

        return stockViews.stream()
            .collect(Collectors.groupingBy(StockView::getProductId))
            .values()
            .stream()
            .map(productStocks -> {
                StockView firstStock = productStocks.get(0);
                ProductStockView view = new ProductStockView();
                view.setProductId(firstStock.getProductId());
                view.setProductName(firstStock.getProductName());
                view.setUnit(firstStock.getUnit());

                List<ProductStockView.WarehouseStock> warehouseStocks = productStocks.stream()
                    .map(stock -> {
                        ProductStockView.WarehouseStock warehouseStock = new ProductStockView.WarehouseStock();
                        warehouseStock.setWarehouseId(stock.getWarehouseId());
                        warehouseStock.setWarehouseName(stock.getWarehouseName());
                        warehouseStock.setQuantity(stock.getQuantity());
                        return warehouseStock;
                    })
                    .collect(Collectors.toList());

                view.setWarehouseStocks(warehouseStocks);
                return view;
            })
            .collect(Collectors.toList());
    }

    public ProductStockView getStockByProduct(Long productId) {
        List<Long> accessibleWarehouseIds = warehouseAccessService.getAccessibleWarehouses()
            .stream()
            .map(Warehouse::getId)
            .collect(Collectors.toList());

        List<StockView> stockViews = stockRepository.findStockByProductAndWarehouses(productId, accessibleWarehouseIds);

        if (stockViews.isEmpty()) {
            return null;
        }

        StockView firstView = stockViews.get(0);
        ProductStockView view = new ProductStockView();
        view.setProductId(firstView.getProductId());
        view.setProductName(firstView.getProductName());
        view.setUnit(firstView.getUnit());

        List<ProductStockView.WarehouseStock> warehouseStocks = stockViews.stream()
            .map(stockView -> {
                ProductStockView.WarehouseStock warehouseStock = new ProductStockView.WarehouseStock();
                warehouseStock.setWarehouseId(stockView.getWarehouseId());
                warehouseStock.setWarehouseName(stockView.getWarehouseName());
                warehouseStock.setQuantity(stockView.getQuantity());
                return warehouseStock;
            })
            .collect(Collectors.toList());

        view.setWarehouseStocks(warehouseStocks);
        return view;
    }
}
