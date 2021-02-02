(ns dev-local
 (:require [datomic.client.api :as d]
           [clojure.string :as str])
 (:import (java.util Date UUID)))


(def dev-local-conf
 {:server-type :dev-local
  :system      "dev"})


(defn client [cfg]
 (d/client cfg))

(def connect
 (memoize
  (fn [db-name]
   (-> (client dev-local-conf)
       (d/connect {:db-name db-name})))))

(defn db [db-name]
 (-> (connect db-name)
     d/db))




(def email-domain-schema
 [{:db/ident       :email/id
   :db/unique      :db.unique/identity
   :db/valueType   :db.type/uuid
   :db/cardinality :db.cardinality/one
   :db/doc         "ID único do email"}

  {:db/ident       :email/date
   :db/valueType   :db.type/instant
   :db/cardinality :db.cardinality/one
   :db/noHistory   true
   :db/doc         "Data de envio"}

  {:db/ident       :email/subject
   :db/valueType   :db.type/string
   :db/cardinality :db.cardinality/one
   :db/doc         "Assunto do email"}

  {:db/ident       :email/sender
   :db/valueType   :db.type/ref
   :db/cardinality :db.cardinality/one
   :db/doc         "Pessoa que fez o envio do email"}
  {:db/ident       :email/recipient
   :db/valueType   :db.type/ref
   :db/cardinality :db.cardinality/many
   :db/doc         "Pessoa(s) que recebem o email"}

  ;;Person schema
  {:db/ident       :person/email
   :db/valueType   :db.type/string
   :db/unique      :db.unique/identity
   :db/cardinality :db.cardinality/one
   :db/doc         "endereço de email de uma pessoa"}

  {:db/ident       :person/full-name
   :db/valueType   :db.type/string
   :db/cardinality :db.cardinality/one
   :db/doc         "Nome da pessoa dona do email"}])


(defn reset-db [db-name]
 (d/delete-database (client dev-local-conf)
                    {:db-name db-name})
 (d/create-database (client dev-local-conf)
                    {:db-name db-name})
 (d/transact (connect db-name)
             {:tx-data email-domain-schema}))

(defn gen-people []
 (let [n (str (rand-nth ["José " "Mário " "Maria " "Jéssica "])
              (rand-nth ["da Silva " "Freitas"
                         "Assunção" "Pereira" "Santos"]))
       m-provider (rand-nth ["@gmail.com" "@paygo.com.br" "@hotmail.com" "@bol.com"])]
  {:person/full-name n
   :person/email     (-> n
                         (str/replace #" " "_")
                         str/lower-case
                         (str m-provider))}))

(defn gen-mail [recipients-number]
 {:email/date      (Date.)
  :email/id        (UUID/randomUUID)
  :email/recipient (->> (range recipients-number)
                        (map (fn [_] (gen-people)))
                        (into []))
  :email/sender    (gen-people)
  :email/subject   (str (rand-nth ["Consolidação " "Preservação " "Estímulo "])
                        (rand-nth ["da fauna " "da flora " "da alfaiataria "])
                        (rand-nth ["brasileira" "argentina" "mesosóica" "fluminense"]))})

(defn gen-data [samples]
 (->> samples
      range
      (map #(mod % 3))
      (map gen-mail)
      (into [])
      (hash-map :tx-data)))

(defn load-db [samples]
 (d/transact (connect "mailing") (gen-data samples)))