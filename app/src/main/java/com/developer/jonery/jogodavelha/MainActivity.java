package com.developer.jonery.jogodavelha;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //Para multiplayer
    private static final int PORTA = 8888;
    public static String TAG = "JogoMultiplayer";

    private boolean isAtivo = true;
    private boolean isRemoto = false;
    private EditText txtRemoto;

    //para single player
    String player = "Jogador 1";
    int counter = 0;
    boolean win;
    GridLayout gameTable;
    ArrayList<View> imageList;
    ImageView linhaResultado;
    char jogadorDaVez;
    char[] jogadas = new char[9];
    int[][] sequencias = {{1,2,3},{4,5,6},{7,8,9},{1,4,7},{2,5,8},{3,6,9},{1,5,9},{3,5,7}};
    Map<Integer, Integer> linhas = new HashMap<>();
    {
        linhas.put(0, R.drawable.linha_horizontal_1);
        linhas.put(1, R.drawable.linha_horizontal_2);
        linhas.put(2, R.drawable.linha_horizontal_3);
        linhas.put(3, R.drawable.linha_vertical_1);
        linhas.put(4, R.drawable.linha_vertical_2);
        linhas.put(5, R.drawable.linha_vertical_3);
        linhas.put(6, R.drawable.linha_decrescente);
        linhas.put(7, R.drawable.linha_crescente);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameTable = (GridLayout) findViewById(R.id.gameTable);
        imageList = gameTable.getTouchables();
        linhaResultado = (ImageView) findViewById(R.id.linhaResultado);
        //para multiplayer
        txtRemoto = (EditText) findViewById(R.id.txtRemoto);

        jogadorDaVez = 'o';
    }

    public void reiniciar(){
        linhaResultado.setImageResource(R.drawable.fundo_transparente);
        counter = 0;
        win = false;
        for(View view : imageList){
            ImageView image = (ImageView) view;
            image.setImageResource(R.drawable.btn_branco);
        }
        jogadas = new char[9];
        for (View casa : imageList){
            casa.setClickable(true);
        }
        //multiplayer
        if (!isRemoto) {
            String servidor = txtRemoto.getText().toString();
            MensageiroTask task = new MensageiroTask();
            task.execute("LIMPAR", servidor, String.valueOf(PORTA));
        }
        //multiplayer
        isRemoto = false;
    }
    public void onClickReiniciar(View v){
        reiniciar();
    }

    public void onClickJogar(View v){
                ImageView btn = (ImageView) v;
                btn.setClickable(false);
        counter += 1;
        String tag = btn.getTag().toString();
        int posicao = Integer.parseInt(tag);
        jogadas[posicao-1] = jogadorDaVez;
        for (int i = 0; i < sequencias.length;i++){
            int[] sequencia = sequencias[i];
            if (jogadas[sequencia[0]-1] == jogadorDaVez&&
                    jogadas[sequencia[1]-1] == jogadorDaVez&&
                    jogadas[sequencia[2]-1] == jogadorDaVez){
                for (View casa : imageList){
                    casa.setClickable(false);
                }
                int idImage = linhas.get(i);
                linhaResultado.setImageResource(idImage);
                win = true;
                Toast.makeText(getApplicationContext(),player + " ganhou", Toast.LENGTH_LONG).show();
                break;
            }
        }
        //multiplayer
        if (!isRemoto) {
            String servidor = txtRemoto.getText().toString();
            MensageiroTask task = new MensageiroTask();
            task.execute(jogadorDaVez + tag, servidor, String.valueOf(PORTA));
        }
        isRemoto = false;

        if (counter ==9&&!win){
            Toast.makeText(getApplicationContext(), "Deu empate", Toast.LENGTH_LONG).show();
        }
                if (jogadorDaVez == 'o') {
                    btn.setImageResource(R.drawable.o_verde_hdpi);
                    player = "Jogador 2";
                    jogadorDaVez = 'x';
                } else {
                    btn.setImageResource(R.drawable.x_preto_hdpi);
                    player = "Jogador 1";
                    jogadorDaVez = 'o';
                }
        }

    private class MensageiroTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String mensagemEnviar = params[0];
            String servidor = params[1];
            int porta = Integer.parseInt(params[2]);
            Socket socket = null;
            DataOutputStream dos = null;
            try {
                socket = new Socket(servidor, porta);
                dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(mensagemEnviar);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAtivo = true;
        new Thread(new BackgroundThread()).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isAtivo = false;
    }

    //multiplayer
    public void processarJogada(String tag){
        int posicao = Integer.parseInt(tag);
        ImageView imageView = (ImageView) imageList.get(posicao -1);
        onClickJogar(imageView);
    }

    //multiplayer
    private class BackgroundThread implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(PORTA);
                while (isAtivo) {
                    Socket socket = null;
                    DataInputStream dis = null;
                    try {
                        socket = serverSocket.accept();
                        dis = new DataInputStream(socket.getInputStream());

                        try {
                            final String msg = dis.readUTF();
                            if (msg != null && !msg.equals("")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        isRemoto = true;
                                        Log.d("Player", msg);
                                        if (msg.equals("LIMPAR")){
                                            reiniciar();
                                        } else {
                                            jogadorDaVez = msg.charAt(0);
                                            processarJogada(String.valueOf(msg.charAt(1)));
                                        }
                                    }
                                });
                            }
                        } catch (IOException e) {
                            //Igonorado;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (dis != null) {
                            try {
                                dis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
