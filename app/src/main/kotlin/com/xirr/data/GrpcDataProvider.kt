package com.xirr.data

import com.xirr.models.PortfolioSnapshot
import grpc.txbofficer.BofficerApiGrpc
import grpc.txbofficer.BofficerProto
import io.grpc.*
import java.time.LocalDate

/**
 * Провайдер данных через gRPC
 */
class GrpcPortfolioDataProvider(
    private val host: String,
    private val port: Int,
    private val authToken: String,
    private val accountId: String,
    private val tradeSystemName: String,
    private val fromDate: LocalDate,
    private val toDate: LocalDate,
    private val currency: String
) : DataProvider {
    
    override fun getPortfolioSnapshots(): List<PortfolioSnapshot> {
        println("Подключение к gRPC: $host:$port")
        
        val channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .intercept(AuthInterceptor(authToken))
            .build()
        
        try {
            val stub = BofficerApiGrpc.newBlockingStub(channel)
            
            val request = BofficerProto.GetPortfolioHistoryRequest.newBuilder()
                .setAccountId(
                    BofficerProto.AccountId.newBuilder()
                        .setId(accountId)
                        .setTradeSystemName(tradeSystemName)
                        .build()
                )
                .setPeriod(
                    BofficerProto.Period.newBuilder()
                        .setFrom(
                            BofficerProto.Date.newBuilder()
                                .setYear(fromDate.year)
                                .setMonth(fromDate.monthValue)
                                .setDay(fromDate.dayOfMonth)
                                .build()
                        )
                        .setTo(
                            BofficerProto.Date.newBuilder()
                                .setYear(toDate.year)
                                .setMonth(toDate.monthValue)
                                .setDay(toDate.dayOfMonth)
                                .build()
                        )
                        .build()
                )
                .setCurrency(currency)
                .build()
            
            println("Отправка gRPC запроса...")
            val response = stub.getPortfolioHistory(request)
            println("Получено дней: ${response.daysCount}")
            
            val snapshots = response.daysList.map { day ->
                PortfolioSnapshot(
                    date = LocalDate.of(day.date.year, day.date.month, day.date.day),
                    valuation = parseDecimalValue(day.equity),
                    cashIn = if (day.hasDeposit()) parseDecimalValue(day.deposit) else 0.0,
                    cashOut = if (day.hasWithdraw()) parseDecimalValue(day.withdraw) else 0.0,
                )
            }
            
            return snapshots
            
        } finally {
            channel.shutdown()
        }
    }
    
    private fun parseDecimalValue(value: BofficerProto.DecimalValue): Double {
        if (value.num.isEmpty()) return 0.0
        val num = value.num.toLong()
        val scale = value.scale
        return num / Math.pow(10.0, scale.toDouble())
    }
    
    /**
     * Interceptor для добавления authorization header
     */
    private class AuthInterceptor(private val token: String) : ClientInterceptor {
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {
            return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)
            ) {
                override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                    if (token.isNotEmpty()) {
                        val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
                        headers.put(authKey, token)
                    }
                    super.start(responseListener, headers)
                }
            }
        }
    }
}