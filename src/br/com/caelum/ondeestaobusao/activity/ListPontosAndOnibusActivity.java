package br.com.caelum.ondeestaobusao.activity;

import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import br.com.caelum.ondeestaobusao.adapter.PontosEOnibusAdapter;
import br.com.caelum.ondeestaobusao.constants.Extras;
import br.com.caelum.ondeestaobusao.delegate.AsyncResultDelegate;
import br.com.caelum.ondeestaobusao.gps.GPSControl;
import br.com.caelum.ondeestaobusao.model.Coordenada;
import br.com.caelum.ondeestaobusao.model.Destino;
import br.com.caelum.ondeestaobusao.model.Onibus;
import br.com.caelum.ondeestaobusao.model.Ponto;
import br.com.caelum.ondeestaobusao.task.DestinosTask;
import br.com.caelum.ondeestaobusao.task.GetJsonAsyncTask;
import br.com.caelum.ondeestaobusao.util.AlertDialogBuilder;
import br.com.caelum.ondeestaobusao.widget.AppRater;

public class ListPontosAndOnibusActivity extends Activity {
	private ExpandableListView lvPontos;
	private Coordenada atual;
	private GPSControl gps;
	
	private AsyncResultDelegate<Coordenada> delegateCoordenada = new AsyncResultDelegate<Coordenada>() {
		@Override
		public void dealWithResult(Coordenada result) {
			Intent intent = new Intent(ListPontosAndOnibusActivity.this, MostraPontosActivity.class);
			intent.putExtra(Extras.LOCALIZACAO, result);
			
			startActivity(intent);
		}
		@Override
		public void dealWithError() {
			ListPontosAndOnibusActivity.this.dealWithError();
		}
	};
	
	private AsyncResultDelegate<List<Ponto>> delegatePontos = new AsyncResultDelegate<List<Ponto>>() {
		@Override
		public void dealWithResult(final List<Ponto> pontos) {
			lvPontos.setAdapter(new PontosEOnibusAdapter(pontos, ListPontosAndOnibusActivity.this));

			lvPontos.setOnChildClickListener(new OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

					Onibus onibus = pontos.get(groupPosition).getOnibuses().get(childPosition);

					Intent intent = new Intent(ListPontosAndOnibusActivity.this, MostraItinerarioActivity.class);
					intent.putExtra(Extras.ONIBUS, onibus);
					intent.putExtra(Extras.LOCALIZACAO, atual);

					startActivity(intent);

					return false;
				}
			});

			hideProgressBar();
		}
		@Override
		public void dealWithError() {
			ListPontosAndOnibusActivity.this.dealWithError();
		}
	};

	private AsyncResultDelegate<Destino> delegateDestino = new AsyncResultDelegate<Destino>() {
		@Override
		public void dealWithResult(final Destino destino) {
			Intent intent = new Intent(ListPontosAndOnibusActivity.this, MostraPontosActivity.class);
			intent.putExtra(Extras.LOCALIZACAO, destino.getCoordenada());
			intent.putExtra(Extras.PONTOS, destino.getPontos());
			
			startActivity(intent);
		}
		@Override
		public void dealWithError() {
			ListPontosAndOnibusActivity.this.dealWithError();
		}
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		AppRater.app_launched(this);
		
		setContentView(R.layout.list_ponto);
		
		gps = new GPSControl(this);
		lvPontos = (ExpandableListView) findViewById(R.id.listPonto);
		
		handleIntent(getIntent());
		
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            new GetJsonAsyncTask<String, Destino>(new DestinosTask(getDelegateDestino())).execute(query);
        } else {
    		atualizar();
        }
	}

	private void hideProgressBar() {
		lvPontos.setVisibility(View.VISIBLE);
		findViewById(R.id.progress_bar).setVisibility(View.GONE);
	}

	public void atualizaLista(View view) {
		findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
		lvPontos.setVisibility(View.GONE);
		atualizar();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_principal, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.procura_no_mapa) {
			Intent intent = new Intent(ListPontosAndOnibusActivity.this, MostraPontosActivity.class);
			intent.putExtra(Extras.LOCALIZACAO, atual);

			startActivity(intent);
			return false;
		} else if (item.getItemId() == R.id.search) {
			onSearchRequested();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (this.atual != null) {
			MenuItem item = menu.findItem(R.id.procura_no_mapa);
			item.setEnabled(true);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public void finish() {
		gps.shutdown();
		super.finish();
	}
	
	private void atualizar() {
		gps.execute();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return this.lvPontos;
	}

	public void setAtual(Coordenada atual) {
		this.atual = atual;
	}

	public void dealWithError() {
		new AlertDialogBuilder(this).build().show();
	}
	
	public AsyncResultDelegate<Coordenada> getDelegateCoordenada() {
		return delegateCoordenada;
	}
	
	public AsyncResultDelegate<List<Ponto>> getDelegatePontos() {
		return delegatePontos;
	}
	
	public AsyncResultDelegate<Destino> getDelegateDestino() {
		return delegateDestino;
	}
	
}
