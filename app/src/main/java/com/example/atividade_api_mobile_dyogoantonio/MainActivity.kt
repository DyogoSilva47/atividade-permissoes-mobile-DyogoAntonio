package com.example.atividade_api_mobile_dyogoantonio

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Eelementos visuais
        val etNomeCarta = findViewById<EditText>(R.id.etNomeCarta)
        val spinnerCor = findViewById<Spinner>(R.id.spinnerCor)
        val spinnerTipo = findViewById<Spinner>(R.id.spinnerTipo)
        val btnBuscar = findViewById<Button>(R.id.btnBuscar)
        val tvResultado = findViewById<TextView>(R.id.tvResultado)
        val ivCarta = findViewById<ImageView>(R.id.ivCarta)

        btnBuscar.setOnClickListener {
            val nome = etNomeCarta.text.toString().trim()
            val corPosicao = spinnerCor.selectedItemPosition
            val tipoPosicao = spinnerTipo.selectedItemPosition

            // Validação: Barra se todos os 3 campos estiverem vazios
            if (nome.isEmpty() && corPosicao == 0 && tipoPosicao == 0) {
                tvResultado.text = "Preencha um nome, uma cor ou um tipo para buscar!"
                return@setOnClickListener
            }

            // Feedback visual de carregamento e limpeza da imagem antiga
            tvResultado.text = "Buscando carta no deck..."
            ivCarta.setImageDrawable(null)

            var query = ""

            // Adiciona o nome
            if (nome.isNotEmpty()) {
                query += nome
            }

            // Adiciona a identidade de cor (id:)
            if (corPosicao != 0) {
                val codigosCores = arrayOf("", "w", "b", "u", "r", "g")
                if (query.isNotEmpty()) query += " "
                query += "id:${codigosCores[corPosicao]}"
            }

            // Adiciona o tipo (t:)
            if (tipoPosicao != 0) {
                // Posições: 1=creature, 2=sorcery, 3=instant, 4=artifact, 5=enchantment, 6=planeswalker, 7=land
                val codigosTipos = arrayOf("", "creature", "sorcery", "instant", "artifact", "enchantment", "planeswalker", "land", "Basic")
                if (query.isNotEmpty()) query += " "
                query += "t:${codigosTipos[tipoPosicao]}"
            }

            val url = Uri.parse("https://api.scryfall.com/cards/random")
                .buildUpon()
                .appendQueryParameter("q", query)
                .build()
                .toString()

            val queue = Volley.newRequestQueue(this)

            val request = object : JsonObjectRequest(
                Request.Method.GET, url, null,
                { response ->
                    try {
                        // Extração dos textos
                        val nomeCarta = response.getString("name")
                        val tipoCarta = response.getString("type_line")
                        val custoMana = response.optString("mana_cost", "Sem custo (Terreno)")

                        val resultadoFormatado = """
                            Nome: $nomeCarta
                            Custo de Mana: $custoMana
                            Tipo: $tipoCarta
                        """.trimIndent()

                        tvResultado.text = resultadoFormatado

                        // pegando a imagem
                        var imageUris = response.optJSONObject("image_uris")

                        // Se não achar a imagem direto, procura dentro das "faces" da carta
                        if (imageUris == null && response.has("card_faces")) {
                            val faces = response.getJSONArray("card_faces")
                            imageUris = faces.getJSONObject(0).optJSONObject("image_uris")
                        }

                        if (imageUris != null) {
                            val urlImagem = imageUris.getString("normal")
                            Picasso.get().load(urlImagem).into(ivCarta)
                        }

                    } catch (e: Exception) {
                        tvResultado.text = "Erro ao processar as informações da carta."
                    }
                },
                { error ->
                    val statusCode = error.networkResponse?.statusCode
                    if (statusCode == 404) {
                        tvResultado.text = "Nenhuma carta encontrada com essa combinação de filtros. Tente novamente!"
                    } else if (statusCode != null) {
                        tvResultado.text = "Erro técnico ($statusCode): O servidor negou o acesso."
                    } else {
                        tvResultado.text = "Sem conexão com a internet ou erro no emulador."
                    }
                }
            ) {
                // Identificação obrigatória exigida pela API do Scryfall
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["User-Agent"] = "AtividadeAppMobile/1.0"
                    headers["Accept"] = "application/json"
                    return headers
                }
            }

            queue.add(request)
        }
    }
}