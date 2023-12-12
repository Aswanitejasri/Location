package sample.cdac.com.location.Server;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitApi {

        @POST("/api/cell_data/save")
        Call<CellData> saveCellData(@Body CellData cellData);
}
