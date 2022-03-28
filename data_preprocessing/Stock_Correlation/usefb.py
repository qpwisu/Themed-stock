import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from tqdm import tqdm
from itertools import islice

class Fb:
    def __init__(self):
        self.cred = credentials.Certificate('stock-collection-d1d31-firebase-adminsdk-62byq-3bd3e38724.json')
        firebase_admin.initialize_app(self.cred)

    def load_corrAll(self,dic):
        self.db = firestore.client()
        print("start load_corrAll")
        doc_ref = self.db.collection(u'corrAll')

        one = dict(islice(dic.items(), 0, 800))
        two = dict(islice(dic.items(), 800, 1600))
        three = dict(islice(dic.items(), 1600, None))
        for code in tqdm(one.keys()):
            dr = doc_ref.document(u'' + str(code))
            dr.set(dic[code])

        self.db = firestore.client()
        doc_ref = self.db.collection(u'corrAll')
        for code in tqdm(two.keys()):
            dr = doc_ref.document(u'' + str(code))
            dr.set(dic[code])

        self.db = firestore.client()
        doc_ref = self.db.collection(u'corrAll')
        for code in tqdm(three.keys()):
            dr = doc_ref.document(u'' + str(code))
            dr.set(dic[code])

    def load_stock(self,df_stock):
        self.db = firestore.client()
        df_stock.index= list(map(str,df_stock.index))
        doc_ref = self.db.collection(u'stockPrice')
        print("start stockPrice")
        for code in tqdm(df_stock.columns):
            dr = doc_ref.document(u'' + str(code))
            dr.set(
                df_stock[str(code)].to_dict()
            )

    def load_name(self,name):
        self.db = firestore.client()
        doc_ref = self.db.collection(u'name')
        dr = doc_ref.document(u'n')
        dr.set({"nm":name})

    def load_endDay(self,day):
        self.db = firestore.client()
        doc_ref = self.db.collection(u'day')
        dr = doc_ref.document(u'dday')
        dr.set({"endDay":day})

    def load_highestCorrelation(self,hc):
        self.db = firestore.client()
        print("start load_highestCorrelation")
        countList = [30, 90, 180, 365,540]
        doc_ref = self.db.collection(u'highestCollection')
        for c in tqdm(countList):
            dr = doc_ref.document(u'' + str(c))
            dr.set(hc[c])

    def load_sectors(self,dic1,dic2):
        self.db = firestore.client()
        print("start load_sectors")
        doc_ref = self.db.collection(u'sectors')
        se = doc_ref.document(u'sectors')
        st = doc_ref.document(u'stock')
        se.set(dic1)
        st.set(dic2)

    def load_sectorsCorrAll(self,dic):
        self.db = firestore.client()
        print("start load_sectorsCorrAll")
        doc_ref = self.db.collection(u'sectorsCorrAll')

        one = dict(islice(dic.items(), 0, 800))
        two = dict(islice(dic.items(), 800, 1600))
        three = dict(islice(dic.items(), 1600, None))
        for code in tqdm(one.keys()):
            dr = doc_ref.document(u'' + str(code))
            dr.set(dic[code])

        self.db = firestore.client()
        doc_ref = self.db.collection(u'sectorsCorrAll')
        for code in tqdm(two.keys()):
            dr = doc_ref.document(u'' + str(code))
            dr.set(dic[code])

        self.db = firestore.client()
        doc_ref = self.db.collection(u'sectorsCorrAll')
        for code in tqdm(three.keys()):
            dr = doc_ref.document(u'' + str(code))
            dr.set(dic[code])


