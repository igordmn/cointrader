package exchange.binance.api.model

class OrderFill {
    /**
     * Price.
     */
    private var price: String? = null

    /**
     * Quantity.
     */
    private var qty: String? = null

    /**
     * Commission.
     */
    private var commission: String? = null

    /**
     * Asset on which commission is taken
     */
    private var commissionAsset: String? = null

    override fun toString(): String {
        return "OrderFill(price=$price, qty=$qty, commission=$commission, commissionAsset=$commissionAsset)"
    }
}