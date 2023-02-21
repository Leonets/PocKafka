package it.guccidigital.models

data class Order(val item: String, val size: String, val price: Int, val color: String, val address: String, val cap: String, val destinationCountry: String) {
    constructor(item: String, size: String, price: Int, color: String, address: String, cap: String, destinationCountry: String, shippingPriority: Boolean, priceSuggestedChange: Int) : this(item,size,price,color,address,cap,destinationCountry)
}
