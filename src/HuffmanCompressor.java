import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

public class HuffmanCompressor {

    static class No {

        No direita;
        No esquerda;
        byte letra;
        int freq;

        public No(int freq) {
            this.freq = freq;
        }

        public No(int freq, byte letra) {
            this.freq = freq;
            this.letra = letra;
        }

        public boolean ehFolha() {
            return this.direita == null && this.esquerda == null;
        }
    }

    static class CodigoHuffman {
        int bits;
        int tamanho;

        public CodigoHuffman(int bits, int tamanho){
            this.bits = bits;
            this.tamanho = tamanho;
        }
    }

    public static void main(String[] args) throws IOException {
        byte[] rawData = new FileInputStream(new File("examples/teste.txt")).readAllBytes();

        if (rawData.length == 0) {
            return;
        }

        int len = rawData.length;

        ArrayList<No> listaLetras = new ArrayList<>();

        int[] frequencias = new int[256];
        for (int i = 0; i < len; i++) {
            frequencias[rawData[i] & 0xFF]++;
        }

        for (int i = 0; i < frequencias.length; i++) {
            if (frequencias[i] != 0) {
                listaLetras.add(new No(frequencias[i], (byte) i));
            }
        }

        No raiz = formaArvore(listaLetras);

        List<Byte> cabecalho;
        List<Byte> textoComprimido;

        if (raiz != null) {
            cabecalho = formaCabecalho(raiz);
            textoComprimido = comprimirTexto(raiz, rawData, len);
        }

        int tamanhoStringOriginal = len;
        String binarioStringOriginal = Integer.toBinaryString(tamanhoStringOriginal);
        String binarioStringOriginal32bits = String.format("%32s", binarioStringOriginal).replace(' ', '0');

        String stringFinal = "jose";

        StringBuilder stringFinalBuilder = new StringBuilder(stringFinal);

        int bitsFaltantes = 8 - (stringFinal.length() % 8);
        if (bitsFaltantes < 8) {
            for (int i = 0; i < bitsFaltantes; i++) {
                stringFinalBuilder.append("0");
            }
        }

        System.out.println(stringFinalBuilder.length() + " bits");
    }

    public static No formaArvore(ArrayList<No> lista) {

        Comparator<No> ordemASerSeguida = Comparator.comparing((No n) -> n.freq).thenComparing(n -> n.letra);

        PriorityQueue<No> fila = new PriorityQueue<>(ordemASerSeguida);

        fila.addAll(lista);

        while (fila.size() > 1) {
            No esq = fila.poll();
            No dir = fila.poll();

            No novoNo = new No(esq.freq + dir.freq);
            novoNo.esquerda = esq;
            novoNo.direita = dir;

            fila.add(novoNo);
        }

        return fila.poll();
    }

    public static List<Byte> formaCabecalho(No raiz) {
        Stack<No> pilha = new Stack<>();
        List<Byte> listaBytes = new ArrayList<>();

        pilha.push(raiz);
        No atual;

        while (!pilha.isEmpty()) {
            atual = pilha.pop();

            if (atual.direita != null) {
                pilha.push(atual.direita);
            }
            if (atual.esquerda != null) {
                pilha.push(atual.esquerda);
            }

            if (atual.ehFolha()) {
                listaBytes.add((byte) 1);

                byte valorConvertido = (byte) (atual.letra & 0xFF);

                //String binario = Integer.toBinaryString(atual.letra & 0xFF);
                //String binarioFormatado = String.format("%8s", binario).replace(' ', '0');
                listaBytes.add(valorConvertido);
            } else {
                listaBytes.add((byte) 0);
            }
        }

        return listaBytes;
    }

    public static HashMap<Byte, String> formaTabelaIterativo(No raiz) {
        HashMap<Byte, String> tabela = new HashMap<>();

        Stack<No> pilha = new Stack<>();

        pilha.push(raiz);
        No atual;

        while (!pilha.isEmpty()) {
            atual = pilha.pop();

            if (atual.direita != null) {
                pilha.push(atual.direita);
            }
            if (atual.esquerda != null) {
                pilha.push(atual.esquerda);
            }

            if (atual.ehFolha()) {
                continue;
            }

        }

        return null;
    }

    private static HashMap<Byte, CodigoHuffman> formaTabelaRecusivo(No raiz) {
        HashMap<Byte, CodigoHuffman> tabela = new HashMap<>();
        List<Byte> lista = new ArrayList<>();

        caminhos(raiz, tabela, lista);

        return tabela;
    }

    private static void caminhos(No atual, HashMap<Byte, CodigoHuffman> tabela, List<Byte> lista) {

        if (atual.ehFolha()) {
            int numTeste = 0;

            for (int i = 0; i < lista.size(); i++) {
                numTeste = numTeste | (lista.get(i) << i);
            }

            tabela.put(atual.letra, new CodigoHuffman(numTeste, lista.size()));
            return;
        }

        lista.add((byte) 0);
        caminhos(atual.esquerda, tabela, lista);
        lista.remove(lista.size() - 1);

        lista.add((byte) 1);
        caminhos(atual.direita, tabela, lista);
        lista.remove(lista.size() - 1);
    }

    public static List<Byte> comprimirTexto(No raiz, byte[] palavras, int tamanhoPalavras) {
        List<Byte> textoComprimido = new ArrayList<>();
        HashMap<Byte, CodigoHuffman> tabela = formaTabelaRecusivo(raiz);

        int buffer = 0;
        int bitsNoBuffer = 0;

        for (int i = 0; i < tamanhoPalavras; i++) {
            byte letraAtual = palavras[i];
            CodigoHuffman resultadoTabela = tabela.get(letraAtual);

            buffer = (buffer << resultadoTabela.tamanho) | resultadoTabela.bits;
            bitsNoBuffer += resultadoTabela.tamanho;

            while(bitsNoBuffer >= 8){
                bitsNoBuffer -= 8;

                byte bytePronto = (byte) (buffer >> bitsNoBuffer);
                textoComprimido.add(bytePronto);

                buffer = buffer & ((1 << bitsNoBuffer) - 1);
            }

        }

        if(bitsNoBuffer > 0){
            buffer = buffer << (8 - bitsNoBuffer);
            textoComprimido.add((byte) buffer);
        }

        return textoComprimido;
    }
}
