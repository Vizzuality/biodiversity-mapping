/*
  Polyfill
*/

if (!Object.assign) {
	Object.defineProperty(Object, 'assign', {
		enumerable: false,
		configurable: true,
		writable: true,
		value: function(target, firstSource) {
			'use strict';
			if (target === undefined || target === null) {
				throw new TypeError('Cannot convert first argument to object');
			}

			var to = Object(target);
			for (var i = 1; i < arguments.length; i++) {
				var nextSource = arguments[i];
				if (nextSource === undefined || nextSource === null) {
					continue;
				}

				var keysArray = Object.keys(Object(nextSource));
				for (var nextIndex = 0, len = keysArray.length; nextIndex < len; nextIndex++) {
					var nextKey = keysArray[nextIndex];
					var desc = Object.getOwnPropertyDescriptor(nextSource, nextKey);
					if (desc !== undefined && desc.enumerable) {
						to[nextKey] = nextSource[nextKey];
					}
				}
			}
			return to;
		}
	});
}


// TODO: This is all global page scope and should be tidied by a JS dev.  It is a quick demo...

// The layer to show on the map
var feature = "mammalsr"
var threshold = 17; // TODO: replace with a better accessor for the Svg current value

function setMapFeature(f) {
  feature = f;
  setMapFilter(threshold);
}

// Sets the map layer filter threshold
function setMapFilter(threshold) {
  map.setFilter('biodiversity', ['<=', feature,  Math.floor(threshold)]);
}

mapboxgl.accessToken = 'pk.eyJ1IjoidGltcm9iZXJ0c29uMTAwIiwiYSI6ImNqM2Ria2FmMzAwMDMycWw3YjFhZTlrdXoifQ.arkRTZM2g-GL2LcGoR8X4g';
var map = new mapboxgl.Map({
  container: 'map',
  zoom: 1,
  style: 'mapbox://styles/timrobertson100/cj3dbigfj00022sqmvpcjguhr', // TODO: cartography please
});

map.on('load', function () {
  map.addLayer({
    "id": "biodiversity",
    "type": "fill",
    "source": {
      type: 'vector',
      tiles: ['http://54.194.24.68/map/spp/richness/{z}/{x}/{y}.mvt?bin=hex&hexPerTile=61&v=5']
    },
    "source-layer": "biodiversity",
    "paint": {
      "fill-color": "#006600",
      "fill-opacity": 0.5,
      "fill-outline-color": "#D8E7A4"
    },
  });
  setMapFilter(17); // TODO: perhaps a better way to couple the Svg and Map init than this?
});



/*
 svg progress drawing
*/
;(function() {
	'use strict';

	function SvgProgress(options) {
		this.options = Object.assign({
			holder: null,
			width: 304
		}, options);
		this.init();
	}

	SvgProgress.prototype = {
		init: function() {
			this.holder = this.options.holder;
			this.progress = +this.holder.getAttribute('data-value');
			this.points = eval(this.holder.getAttribute('data-points'));
			this.mobilePoints = eval(this.holder.getAttribute('data-mobile-points'));

			for (var i = 0; i <= 100; i++) {
				if (!this.values) {
					this.values = [];
				}
				this.values.push(360 / 100);
			}

			this.box = document.createElement('div');
			this.box.className = 'box';
			this.holder.appendChild(this.box);

			this.value = document.createElement('span');
			this.value.className = 'value';
			this.box.appendChild(this.value);

			this.title = document.createElement('span');
			this.title.className = 'title';
			this.title.innerHTML = this.holder.dataset.text;
			this.box.appendChild(this.title);

			this.arrowDown = document.createElement('button');
			this.arrowDown.className = 'icon-chevron-down';
			this.arrowDown.setAttribute('arrow', 'down');
			this.box.appendChild(this.arrowDown);

			this.arrowUp = document.createElement('button');
			this.arrowUp.className = 'icon-chevron-up';
			this.arrowUp.setAttribute('arrow', 'up');
			this.box.appendChild(this.arrowUp);

			this.createWheel();
			this.createScale();
			this.refreshActive();
			this.attachEvents();
		},
		refreshActive: function() {
			var self = this;
			var scaleRadius = (this.holder.offsetWidth * 1.5) / 2;

			var lineScaleStartArc = d3.arc()
				.outerRadius(scaleRadius * 0.85)
				.innerRadius(scaleRadius * 0.85);

			var lineScaleEndArc = d3.arc()
				.outerRadius(scaleRadius * 0.885)
				.innerRadius(scaleRadius * 0.885);

			var lineScaleActiveArc = d3.arc()
				.outerRadius(scaleRadius * 0.935)
				.innerRadius(scaleRadius * 0.935);

			var lineScalePointArc = d3.arc()
				.outerRadius(scaleRadius)
				.innerRadius(scaleRadius);

			var getLineClass = function(d, index) {
				var className = 'line';
				if (self.points.indexOf(index) > -1) {
					className += ' point';
				}
				if (self.mobilePoints.indexOf(index) > -1) {
					className += ' mobile-point';
				}
				if (index < self.progress) {
					className += ' pre-active';
				}
				if (index === self.progress) {
					className += ' active';
				}
				return className;
			};
			var getWheelLinePoints = function(d, index) {
				var endPoint = self.lineWheelStartArc.centroid(d);
				var startPoint = self.lineWheelEndArc.centroid(d);
				if (index === self.progress) {
					startPoint = self.lineWheelActiveArc.centroid(d);
				}
				return [endPoint, startPoint];
			};
			var getScaleLinePoints = function(d, index) {
				var endPoint = lineScaleStartArc.centroid(d);
				var startPoint = lineScaleEndArc.centroid(d);
				if (index === self.progress) {
					startPoint = lineScaleActiveArc.centroid(d);
				}
				if (self.mobilePoints.indexOf(index) > -1) {
					startPoint = lineScalePointArc.centroid(d);
				}
				return [endPoint, startPoint];
			};
			this.value.innerHTML = this.progress + '%';
			this.linesWheel.attr('class', getLineClass).attr('points', getWheelLinePoints);
			this.linesScale.attr('class', getLineClass).attr('points', getScaleLinePoints);

            // update the map
            if (map.loaded()) {
              setMapFilter(this.progress)
            }

		},
		createWheel: function() {
			var width = this.options.width;
			var radius = width / 2;
			var pie = d3.pie()
				.sort(null)
				.startAngle(- Math.PI * 0.78)
				.endAngle(Math.PI * 0.78)
				.value(function(d) {
					return d;
				});

			this.svgWheel = d3.select(this.holder).append('svg')
				.attr('class', 'svg-box')
				.attr('width', width)
				.attr('height', width);

			this.svgWheel.append('circle')
				.attr('class', 'bg-wheel')
				.attr('cx', radius)
				.attr('cy', radius)
				.attr('r', radius - 25);

			this.svgWheel.append('line')
				.attr('class', 'separator')
				.attr('x1', radius)
				.attr('y1', width - 50)
				.attr('x2', radius)
				.attr('y2', width);

			this.lineWheelStartArc = d3.arc()
				.outerRadius(radius - 7)
				.innerRadius(radius - 7);

			this.lineWheelEndArc = d3.arc()
				.outerRadius(radius - 43)
				.innerRadius(radius - 43);

			this.lineWheelActiveArc = d3.arc()
				.outerRadius(radius - 56)
				.innerRadius(radius - 56);

			var lineGroup = this.svgWheel.append('g')
				.attr('transform', 'translate('+ radius +','+ radius +')')
				.selectAll('.wheel-arc')
				.data(pie(this.values))
				.enter().append('g')
				.attr('class', 'wheel-arc');

			var linePie = d3.arc()
				.outerRadius(radius - 7)
				.innerRadius(radius - 43);

			this.linesWheel = lineGroup.append('polyline')
				.attr('data-index', function(d, index) {
					return index;
				});

			lineGroup.append('path')
				.attr('d', linePie)
				.attr('data-index', function(d, index) {
					return index;
				});
		},
		createScale: function() {
			var width = this.holder.offsetWidth * 1.5;
			var radius = width / 2;

			var pie = d3.pie()
				.sort(null)
				.startAngle(- Math.PI * 0.212)
				.endAngle(Math.PI * 0.212)
				.value(function(d) {
					return d;
				});

			var linePie = d3.arc()
				.outerRadius(radius * 0.85)
				.innerRadius(radius);

			this.svgScale = d3.select(this.holder).append('svg')
				.attr('class', 'svg-scale')
				.attr('width', width)
				.attr('height', width * 0.17);

			this.svgScale.append('g')
				.attr('class', 'bg-scale-group')
				.attr('transform', 'translate('+ radius +','+ radius +')')
				.append('path')
				.data(pie([1]))
				.attr('d', linePie)
				.attr('class', 'bg-scale');

			this.lineScaleGroup = this.svgScale.append('g')
				.attr('class', 'scale-lines')
				.attr('transform', 'translate('+ radius +','+ radius +')')
				.selectAll('.scale-arc')
				.data(pie(this.values))
				.enter().append('g')
				.attr('class', 'scale-arc');

			this.lineScalePath = this.lineScaleGroup.append('path')
				.attr('d', linePie)
				.attr('data-index', function(d, index) {
					return index;
				});

			this.linesScale = this.lineScaleGroup.append('polyline')
				.attr('data-index', function(d, index) {
					return index;
				});
		},
		levelUp: function() {
			this.progress++;
			if (this.progress > 100) {
				this.progress = 100;
			}
			this.refreshActive();
		},
		levelDown: function() {
			this.progress--;
			if (this.progress < 0) {
				this.progress = 0;
			}
			this.refreshActive();
		},
		onDrag: function(d) {
			var index = d3.event.sourceEvent.target.getAttribute('data-index');
			if (index) {
				this.progress = +index;
				this.refreshActive();
			}
		},
		resizeHandler: function() {
			var width = this.holder.offsetWidth * 1.5;
			var radius = width / 2;
			var linePie = d3.arc()
				.outerRadius(radius * 0.85)
				.innerRadius(radius);

			this.svgScale.attr('width', width)
				.attr('height', width * 0.17)
				.selectAll('.scale-lines')
				.attr('transform', 'translate('+ radius +','+ radius +')');

			this.svgScale.selectAll('.bg-scale-group')
				.attr('transform', 'translate('+ radius +','+ radius +')')
				.selectAll('.bg-scale')
				.attr('d', linePie);


			this.lineScalePath.attr('d', linePie);
			this.refreshActive();
		},
		attachEvents: function() {
			this.svgWheel.call(d3.drag().on('drag start', this.onDrag.bind(this)));
			this.svgScale.call(d3.drag().on('drag start', this.onDrag.bind(this)));
			this.arrowDown.addEventListener('click', this.levelDown.bind(this));
			this.arrowUp.addEventListener('click', this.levelUp.bind(this));
			window.addEventListener('resize', this.resizeHandler.bind(this));
		}
	};

	var boxes = document.querySelectorAll('.holder-settings-wheel');

	for (var i = 0; i < boxes.length; i++) {
		new SvgProgress({holder: boxes[0]});
	}
}());

/*
 legend popup
*/
;(function() {
	'use strict';

	function OpenClose(options) {
		this.options = Object.assign({
			opener: null,
			closer: 'icon-cross',
			activeClass: 'active'
		}, options);
		this.init();
	}

	OpenClose.prototype = {
		init: function() {
			this.opener = this.options.opener;
			this.popup = document.getElementById(this.opener.getAttribute('href').substr(1));
			this.closer = this.popup.getElementsByClassName(this.options.closer)[0];
			this.attachEvents();
			if (window.innerWidth >= 1024) {
				this.popup.classList.add(this.options.activeClass);
			}
		},
		showPopup: function(event) {
			event.preventDefault();
			this.popup.classList.add(this.options.activeClass);
		},
		hidePopup: function(event) {
			event.preventDefault();
			this.popup.classList.remove(this.options.activeClass);
		},
		attachEvents: function() {
			this.opener.addEventListener('click', this.showPopup.bind(this), false);
			this.closer.addEventListener('click', this.hidePopup.bind(this), false);
		}
	};

	var openers = document.querySelectorAll('.opener-legend');

	for (var i = 0; i < openers.length; i++) {
		new OpenClose({opener: openers[0]});
	}
}());
