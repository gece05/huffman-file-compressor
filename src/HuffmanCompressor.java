
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    static class Codigo {

        int bits;
        int tamanho;

        public Codigo(int bits, int tamanho) {
            this.bits = bits;
            this.tamanho = tamanho;
        }
    }

    public static void main(String[] args) throws IOException {
        String tipo = args[0];
        String nomeArquivo = args[1];

        if (args.length < 2) {
            System.err.println("Usage: java HuffmanCompressor <compress|decompress> <filename>");
            return;
        }

        File file = new File(nomeArquivo);

        FileInputStream fis = new FileInputStream(file);

        byte[] rawData = fis.readAllBytes();
        int len = rawData.length;

        if (len == 0) {
            fis.close();
            return;
        }

        switch (tipo) {
            case "compress" -> {
                compress(rawData, len, nomeArquivo);
                System.out.println("Arquivo comprimido com sucesso.");
                break;
            }
            case "decompress" -> {
                break;
            }
            default -> {
                System.out.println("Formato desconhecido.");
                fis.close();
                return;
            }
        }

        fis.close();
    }

    public static void compress(byte[] rawData, int len, String nomeArquivo) throws FileNotFoundException {
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

        List<Byte> cabecalho = formaCabecalho(raiz);
        List<Byte> textoComprimido = comprimirTexto(raiz, rawData, len);

        byte bitsFaltando = textoComprimido.remove(textoComprimido.size() - 1);

        int tamArquivoFinal = cabecalho.size() + textoComprimido.size() + 1;

        byte[] bytesFinais = new byte[tamArquivoFinal];

        int ponteiro = 0;

        bytesFinais[ponteiro++] = bitsFaltando;

        for (Byte b : cabecalho) {
            bytesFinais[ponteiro++] = b;
        }

        for (Byte b : textoComprimido) {
            bytesFinais[ponteiro++] = b;
        }

        File outFile = new File("src\\out\\", "arquivoTeste.huffman");
        FileOutputStream fos = new FileOutputStream(outFile);

        makeDir();
        try {
            fos.write(bytesFinais);
            fos.close();
        } catch (IOException ex) {
            System.err.println("Error writing compressed file: " + ex.getMessage());
            ex.printStackTrace();
        }
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

    private static HashMap<Byte, Codigo> formaTabelaRecusivo(No raiz) {
        HashMap<Byte, Codigo> tabela = new HashMap<>();
        List<Byte> lista = new ArrayList<>();

        caminhos(raiz, tabela, lista);

        return tabela;
    }

    private static void caminhos(No atual, HashMap<Byte, Codigo> tabela, List<Byte> lista) {

        if (atual.ehFolha()) {
            int numTeste = 0;

            for (int i = 0; i < lista.size(); i++) {
                numTeste = numTeste | (lista.get(i) << i);
            }

            tabela.put(atual.letra, new Codigo(numTeste, lista.size()));
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
        HashMap<Byte, Codigo> tabela = formaTabelaRecusivo(raiz);

        int buffer = 0;
        int bitsNoBuffer = 0;
        int bitsFaltando = 0;

        for (int i = 0; i < tamanhoPalavras; i++) {
            byte letraAtual = palavras[i];
            Codigo resultadoTabela = tabela.get(letraAtual);

            buffer = (buffer << resultadoTabela.tamanho) | resultadoTabela.bits;
            bitsNoBuffer += resultadoTabela.tamanho;

            while (bitsNoBuffer >= 8) {
                bitsNoBuffer -= 8;

                byte bytePronto = (byte) (buffer >> bitsNoBuffer);
                textoComprimido.add(bytePronto);

                buffer = buffer & ((1 << bitsNoBuffer) - 1);
            }

        }

        if (bitsNoBuffer > 0) {
            bitsFaltando = 8 - bitsNoBuffer;
            buffer = buffer << bitsFaltando;
            textoComprimido.add((byte) buffer);
        }

        textoComprimido.add((byte) bitsFaltando);
        return textoComprimido;
    }

    private static void makeDir() {
        File theDir = new File(System.getProperty("user.dir") + File.separator + "out");

        if (!theDir.exists()) {
            try {
                theDir.mkdir();
            } catch (Exception e) {
                // TODO: Lidar com isso aqui ner
            }
        }

    }
}
