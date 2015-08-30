package felipe.com.br.aguaparatodos.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import felipe.com.br.aguaparatodos.R;
import felipe.com.br.aguaparatodos.dominio.Usuario;
import felipe.com.br.aguaparatodos.utils.BuscarEnderecoGoogle;
import felipe.com.br.aguaparatodos.utils.ToastUtil;
import felipe.com.br.aguaparatodos.utils.UsuarioSingleton;
import felipe.com.br.aguaparatodos.utils.ValidadorUtil;
import felipe.com.br.aguaparatodos.utils.WebService;

/**
 * Created by felipe on 8/24/15.
 */
public class RegistrarOcorrencia extends AppCompatActivity {

    private static final String LOG_TAG = "Projeto Agua para Todos";

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String PAIS = "br";
    private static final String LANGUAGE = "pt-BR";
    private static final String GOOGLE_API_KEY = "AIzaSyApev4-PxnD258_TnkDCcCL_KTOXwhjU7M";
    private AutoCompleteTextView enderecoAutoComplete;

    private EditText tituloOcorrencia, observacaoOcorrencia, pontoReferenciaOcorrencia;
    private Button btnCadastrarOcorrencia;

    private RequestParams parametros;
    private static ProgressDialog progressDialog;

    private Drawer navigationDrawer;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registrar_ocorrencia);

        criarReferenciasComponentes();

        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.toolbar.setTitle(getResources().getString(R.string.tituloTelaRegistrarOcorrenia));
        //this.toolbar.setBackgroundColor(getResources().getColor(R.color.vermelho));
        this.toolbar.setTitleTextColor(getResources().getColor(R.color.branco));
        setSupportActionBar(this.toolbar);

        this.navigationDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(this.toolbar)
                .build();

        this.navigationDrawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        this.toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegistrarOcorrencia.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

    }

    private void criarReferenciasComponentes() {
        this.enderecoAutoComplete = (AutoCompleteTextView) findViewById(R.id.editTextEnderecoOcorrencia);
        this.enderecoAutoComplete.setAdapter(new PlacesAutoCompleteAdapter(getApplicationContext(), R.layout.item_lista_busca_endereco));

        this.tituloOcorrencia = (EditText) findViewById(R.id.editTextTituloOcorrencia);
        this.observacaoOcorrencia = (EditText) findViewById(R.id.editTextDescricaoOcorrencia);
        this.pontoReferenciaOcorrencia = (EditText) findViewById(R.id.editTextPontoReferenciaOcorrencia);

        this.btnCadastrarOcorrencia = (Button) findViewById(R.id.btnEnviarOcorrencia);
        this.btnCadastrarOcorrencia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog = ProgressDialog.show(RegistrarOcorrencia.this, getResources().getString(R.string.aguarde),
                        getResources().getString(R.string.msgCadastrandoOcorrencia));

                tituloOcorrencia.setError(null);
                //observacaoOcorrencia.setError(null);
                enderecoAutoComplete.setError(null);
                //pontoReferenciaOcorrencia.setError(null);

                ValidadorUtil.validarCampoEmBranco(tituloOcorrencia, getResources().getString(R.string.erroInformarTituloOcorrencia));
                ValidadorUtil.validarCampoEmBranco(enderecoAutoComplete, getResources().getString(R.string.erroInformarTituloOcorrencia));

                if (ValidadorUtil.isNulo(tituloOcorrencia.getError()) && ValidadorUtil.isNulo(enderecoAutoComplete.getError())) {
                    prepararParametros();
                }
            }
        });
    }

    private void prepararParametros() {
        try {
            List<Double> coordenadasEndereco = BuscarEnderecoGoogle.buscarCoordenadasPorEndereco(getApplicationContext(), this.enderecoAutoComplete.getText().toString());
            Map<String, String> valores = BuscarEnderecoGoogle.buscarEnderecoByNome(enderecoAutoComplete.getText().toString(), getApplicationContext());

            Log.d("ENDERECO", valores.get("ENDERECO"));
            Log.d("CIDADE", valores.get("CIDADE"));
            Log.d("ESTADO", valores.get("ESTADO"));

            this.parametros = new RequestParams();

            this.parametros.put("titulo", this.tituloOcorrencia.getText().toString());
            this.parametros.put("descricao", this.observacaoOcorrencia.getText().toString());
            this.parametros.put("referencia", this.pontoReferenciaOcorrencia.getText().toString());
            this.parametros.put("latitude", String.valueOf(coordenadasEndereco.get(0)));
            this.parametros.put("longitude", String.valueOf(coordenadasEndereco.get(1)));
            this.parametros.put("id_usuario", String.valueOf(UsuarioSingleton.getInstancia().getUsuario().getId()));
            this.parametros.put("endereco", String.valueOf(valores.get("ENDERECO")));
            this.parametros.put("cidade", String.valueOf(valores.get("CIDADE")));
            this.parametros.put("estado", String.valueOf(valores.get("ESTADO")));

            this.enviarOcorrencia(this.parametros);
        } catch (Exception e) {
            ToastUtil.criarToastLongo(getApplicationContext(), getResources().getString(R.string.erroBuscarEndereco));
        }


    }

    private boolean enviarOcorrencia(RequestParams parametros) {
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(WebService.ENDERECO_WS.concat(getResources().getString(R.string.ocorrencia_nova)), parametros, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                String str = "";
                try {
                    str = new String(bytes, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (str.equalsIgnoreCase("sucesso")) {
                    progressDialog.dismiss();

                    ToastUtil.criarToastLongo(getApplicationContext(),
                            getResources().getString(R.string.msgSucessoEnviarOcorrencia));

                    limparCampos();

                    startActivity(new Intent(RegistrarOcorrencia.this, MainActivity.class));
                } else if (str.equalsIgnoreCase("erro")) {
                    progressDialog.dismiss();

                    ToastUtil.criarToastLongo(getApplicationContext(),
                            getResources().getString(R.string.msgErroEnviarOcorrencia));
                }

            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                progressDialog.dismiss();

                ToastUtil.criarToastLongo(getApplicationContext(), getResources().getString(R.string.msgErroWS));
            }

        });

        return true;
    }

    private void limparCampos() {
        this.tituloOcorrencia.setText("");
        this.observacaoOcorrencia.setText("");
        this.enderecoAutoComplete.setText("");
        this.pontoReferenciaOcorrencia.setText("");
    }

    @Override
    public void onBackPressed() {
        this.navigationDrawer.setSelection(0);
        super.onBackPressed();
    }

    private class PlacesAutoCompleteAdapter extends ArrayAdapter<String>
            implements Filterable {
        private ArrayList<String> resultList;

        public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint,
                                              FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

    private ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE
                    + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + GOOGLE_API_KEY);
            sb.append("&components=country:" + PAIS);
            sb.append("&language=" + LANGUAGE);
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString(
                        "description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }
}