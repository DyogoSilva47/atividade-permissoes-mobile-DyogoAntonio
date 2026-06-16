package com.example.atividade_api_mobile_dyogoantonio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var etNomeCarta: EditText
    private lateinit var spinnerCor: Spinner
    private lateinit var spinnerTipo: Spinner
    private lateinit var tvResultado: TextView
    private lateinit var ivCarta: ImageView

    private val CHANNEL_ID = "alerta_precos_magic"

    // Guardando os dados da última busca para usar no balão de notificação
    private var ultimoNomeBuscado = ""
    private var ultimoPrecoBuscado = ""

    // LÓGICA DA NOTIFICAÇÃO: O que fazer com a resposta da permissão do usuário
    private val pedirPermissaoLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permissão concedida: Dispara a notificação na hora
            dispararNotificacao()
        } else {
            // Permissão negada: Mostra o aviso original amigável sem quebrar o app
            Toast.makeText(this, "Permissão negada. Não poderemos te avisar sobre mudanças de preço.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa o canal de notificações (Obrigatório para sistemas Android mais novos)
        criarCanalNotificacao()

        // Conectando os elementos visuais
        etNomeCarta = findViewById(R.id.etNomeCarta)
        spinnerCor = findViewById(R.id.spinnerCor)
        spinnerTipo = findViewById(R.id.spinnerTipo)
        tvResultado = findViewById(R.id.tvResultado)
        ivCarta = findViewById(R.id.ivCarta)

        val btnBuscar = findViewById<Button>(R.id.btnBuscar)
        val btnMonitorarPreco = findViewById<Button>(R.id.btnMonitorarPreco)

        btnBuscar.setOnClickListener {
            realizarBusca()
        }

        // NOVO BOTÃO DE ALERTA: Gerencia o fluxo de permissão em runtime
        btnMonitorarPreco.setOnClickListener {
            if (ultimoNomeBuscado.isEmpty()) {
                Toast.makeText(this, "Busque uma carta primeiro para monitorar o preço!", Toast.LENGTH_SHORT).show()
            } else {
                verificarPermissaoEEnviarNotificacao()
            }
        }
    }

    private fun verificarPermissaoEEnviarNotificacao() {
        // A checagem de runtime de notificação é obrigatória a partir do Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissao = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED) {
                dispararNotificacao()
            } else {
                pedirPermissaoLauncher.launch(permissao)
            }
        } else {
            // Aparelhos mais antigos concedem direto na instalação
            dispararNotificacao()
        }
    }

    private fun dispararNotificacao() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // Ícone de sino padrão do sistema
            .setContentTitle("Alerta de Preço Ativado!")
            .setContentText("Acompanhando variações de preço para $ultimoNomeBuscado (USD $ultimoPrecoBuscado)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Preço"
            val descriptionText = "Canal para monitoramento de valores das cartas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun realizarBusca() {
        val nome = etNomeCarta.text.toString().trim()
        val corPosicao = spinnerCor.selectedItemPosition
        val tipoPosicao = spinnerTipo.selectedItemPosition

        // Sua validação original intacta
        if (nome.isEmpty() && corPosicao == 0 && tipoPosicao == 0) {
            tvResultado.text = "Preencha um nome, uma cor ou um tipo para buscar!"
            return
        }

        // Seus textos originais de feedback visual
        tvResultado.text = "Buscando carta no deck..."
        ivCarta.setImageDrawable(null)

        var query = ""

        if (nome.isNotEmpty()) {
            query += nome
        }

        if (corPosicao != 0) {
            val codigosCores = arrayOf("", "w", "b", "u", "r", "g")
            if (query.isNotEmpty()) query += " "
            if (tipoPosicao == 7 || tipoPosicao == 8) {
                query += "id:${codigosCores[corPosicao]}"
            } else {
                query += "c:${codigosCores[corPosicao]}"
            }
        }

        if (tipoPosicao != 0) {
            val codigosTipos = arrayOf("", "creature", "sorcery", "instant", "artifact", "enchantment", "planeswalker", "land", "basic")
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
                    val nomeCarta = response.getString("name")
                    val tipoCarta = response.getString("type_line")
                    val custoMana = response.optString("mana_cost", "Sem custo (Terreno)")

                    // Coletando o valor em dólares direto da API
                    val prices = response.optJSONObject("prices")
                    val precoUSD = prices?.optString("usd") ?: "Indisponível"

                    // Atualiza as referências locais para a notificação usar quando o botão for clicado
                    ultimoNomeBuscado = nomeCarta
                    ultimoPrecoBuscado = precoUSD

                    // Sua estrutura de texto original expandida com a linha do preço real
                    val resultadoFormatado = """
                        Nome: $nomeCarta
                        Custo de Mana: $custoMana
                        Tipo: $tipoCarta
                        Preço Atual: USD $precoUSD
                    """.trimIndent()

                    tvResultado.text = resultadoFormatado

                    var imageUris = response.optJSONObject("image_uris")

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
                // Suas validações de erros técnicas mantidas iguaizinhas
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