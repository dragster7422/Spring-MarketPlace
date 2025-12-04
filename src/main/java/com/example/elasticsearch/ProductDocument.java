package com.example.elasticsearch;

import com.example.models.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(indexName = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime dateOfCreated;

    @Field(type = FieldType.Keyword)
    private String previewImageUrl;

    @Field(type = FieldType.Long)
    private Long ownerId;

    @Field(type = FieldType.Keyword)
    private String ownerUsername;

    // Constructor from Product entity
    public ProductDocument(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.dateOfCreated = product.getDateOfCreated();
        this.previewImageUrl = product.getPreviewImageUrl();

        if (product.getOwner() != null) {
            this.ownerId = product.getOwner().getId();
            this.ownerUsername = product.getOwner().getUsername();
        }
    }
}