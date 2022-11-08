package br.sc.weg.sid.model.entities;

public enum Moeda {
    EURO("Euro"),
    DOLAR("Dólar"),
    REAL("Real");

    String nome;
    Moeda(String nome){
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
